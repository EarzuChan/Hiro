package me.earzuchan.hiro.compose.internal

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import me.earzuchan.hiro.compose.internal.architecture.HiroSavedStateTransport
import me.earzuchan.hiro.compose.internal.input.HiroComposePointerEvent
import me.earzuchan.hiro.compose.internal.input.HiroImeCommandSink
import me.earzuchan.hiro.compose.internal.focus.HiroAndroidFocusBridge
import me.earzuchan.hiro.compose.internal.input.HiroImeEditRequest
import me.earzuchan.hiro.compose.internal.input.HiroImeHost
import me.earzuchan.hiro.compose.savable.HiroSavableStateConfiguration
import me.earzuchan.hiro.compose.windowinsets.HiroWindowInsetsSnapshot
import me.earzuchan.hiro.skia.HiroSkiaLayer
import me.earzuchan.hiro.skia.HiroSkiaRenderDelegate
import me.earzuchan.hiro.skia.HiroSkiaRenderLifecycleDelegate
import org.jetbrains.skia.Canvas
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private data class HiroComposePlatformState(
    val environment: HiroComposeEnvironment,
    val viewport: IntSize?,
    val windowInsets: HiroWindowInsetsSnapshot,
    val inputMode: InputMode?,
)

internal class HiroComposeRenderController(private val layer: HiroSkiaLayer, initialEnvironment: HiroComposeEnvironment, initialWindowInsets: HiroWindowInsetsSnapshot, private val requestInputMode: (InputMode) -> Boolean, private val requestNavigationBackHandling: (Boolean) -> Boolean, private val savedStateTransport: HiroSavedStateTransport, private val savableStateConfiguration: HiroSavableStateConfiguration, private val imeHost: HiroImeHost, private val focusBridge: HiroAndroidFocusBridge, private val androidPlatformServices: HiroAndroidPlatformServices) : HiroSkiaRenderDelegate, HiroSkiaRenderLifecycleDelegate, HiroImeCommandSink {
    private val commands = HiroComposeCommandMailbox()
    private val drainScheduled = AtomicBoolean(false)
    private val platformStateDirty = AtomicBoolean(true)
    private val state = AtomicReference(HiroComposeRenderState.WaitingForRenderThread)
    private val latestPlatformState = AtomicReference(
        HiroComposePlatformState(
            environment = initialEnvironment,
            viewport = null,
            windowInsets = initialWindowInsets,
            inputMode = null,
        )
    )
    private val dispatcher = HiroSkiaRenderDispatcher(layer::isOnRenderThread, layer::queueToRenderThread)

    private var scene: HiroSkiaComposeScene? = null
    private var dispatcherRegistration: AutoCloseable? = null
    private var terminated = false

    fun setContent(content: @Composable () -> Unit) = post(HiroComposeCommand.SetContent(content))

    fun updateEnvironment(environment: HiroComposeEnvironment) = updatePlatformState { it.copy(environment = environment) }

    fun updateViewport(size: IntSize) = updatePlatformState { it.copy(viewport = size) }

    fun updateWindowInsets(snapshot: HiroWindowInsetsSnapshot) = updatePlatformState { it.copy(windowInsets = snapshot) }

    fun updateInputMode(inputMode: InputMode) = updatePlatformState { it.copy(inputMode = inputMode) }

    fun updateLifecycle(state: Lifecycle.State): Boolean = post(HiroComposeCommand.MoveLifecycle(state))

    fun sendPointerEvent(event: HiroComposePointerEvent): Boolean = post(HiroComposeCommand.PointerEvent(event))

    fun cancelPointerInput() = post(HiroComposeCommand.CancelPointerInput)

    fun releaseFocus() = post(HiroComposeCommand.ReleaseFocus)

    fun takeFocus(direction: FocusDirection): Boolean = post(HiroComposeCommand.TakeFocus(direction))

    fun dispatchNavigationBack(): Boolean = post(HiroComposeCommand.NavigationBack)

    fun dispatchViewKeyEvent(event: KeyEvent): Boolean {
        val request = HiroViewKeyEventRequest(event)
        if (!post(HiroComposeCommand.ViewKeyInput(request))) return false
        return when (request.await(VIEW_KEY_EVENT_TIMEOUT_MILLIS)) {
            HiroViewKeyEventRequest.Result.Handled -> true
            HiroViewKeyEventRequest.Result.Unhandled,
            HiroViewKeyEventRequest.Result.Cancelled -> false
            HiroViewKeyEventRequest.Result.ClaimedTimeout -> {
                Log.w(TAG, "View 键事件已被渲染线程认领但未及时完成，按已消费处理")
                true
            }
        }
    }

    override fun enqueueImeEdit(request: HiroImeEditRequest): Boolean = post(HiroComposeCommand.ImeEdit(request))

    override fun enqueueImeAction(sessionId: Long, action: ImeAction): Boolean = post(HiroComposeCommand.ImeAction(sessionId, action))

    override fun enqueueImeKeyEvent(event: KeyEvent): Boolean = post(HiroComposeCommand.ImeKeyInput(event))

    fun wake() {
        signalDrain()
        layer.needRender()
    }

    fun onHostResumeOnRenderThread() {
        val previous = dispatcher.enterCurrent()
        try {
            checkRenderThread()

            if (!state.get().acceptsCommands) return
            if (latestPlatformState.get().viewport == null) return
            drainCommands()
            if (!state.get().acceptsCommands) return
            ensureScene()
        } finally {
            dispatcher.leaveCurrent(previous)
        }
    }

    fun onHostPauseOnRenderThread() {
        val previous = dispatcher.enterCurrent()
        try {
            checkRenderThread()

            if (!state.get().acceptsCommands) return
            if (scene == null) return
            drainCommands()
            if (!state.get().acceptsCommands) return
            scene?.checkpointSavedState()
        } finally {
            dispatcher.leaveCurrent(previous)
        }
    }

    fun beginClose() {
        while (true) {
            val current = state.get()
            if (current == HiroComposeRenderState.Closing || current == HiroComposeRenderState.Closed) return
            if (state.compareAndSet(current, HiroComposeRenderState.Closing)) return
        }
    }

    fun closeBeforeRenderThreadStarts() {
        beginClose()

        commands.clear()
        dispatcherRegistration?.close()
        dispatcherRegistration = null
        dispatcher.close()
        terminated = true
        state.set(HiroComposeRenderState.Closed)
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        val previous = dispatcher.enterCurrent()
        try {
            checkRenderThread()

            if (terminated || state.get() == HiroComposeRenderState.Closing) return
            check(width >= 0 && height >= 0) { "Compose Skia Android 渲染尺寸不能为负数" }
            val renderSize = IntSize(width, height)
            if (latestPlatformState.get().viewport != renderSize) {
                latestPlatformState.updateAndGet { it.copy(viewport = renderSize) }
                platformStateDirty.set(true)
            }
            drainCommands()
            scene?.render(canvas, nanoTime)
        } finally {
            dispatcher.leaveCurrent(previous)
        }
    }

    override fun onRenderThreadClosing() {
        val previous = dispatcher.enterCurrent()
        try {
            checkRenderThread()
            if (terminated) return

            state.set(HiroComposeRenderState.Closing)
            commands.clear()
            var failure: Throwable? = null

            try {
                dispatcherRegistration?.close()
            } catch (throwable: Throwable) {
                failure = throwable
            }

            dispatcherRegistration = null
            try {
                scene?.close()
            } catch (throwable: Throwable) {
                failure?.addSuppressed(throwable) ?: run { failure = throwable }
            }

            scene = null
            dispatcher.close()
            terminated = true
            state.set(HiroComposeRenderState.Closed)
            failure?.let { throw it }
        } finally {
            dispatcher.leaveCurrent(previous)
        }
    }

    private fun post(command: HiroComposeCommand): Boolean {
        if (!state.get().acceptsCommands) return false

        commands.add(command)
        signalDrain()
        layer.needRender()
        return true
    }

    private fun signalPlatformStateChanged(): Boolean {
        if (!state.get().acceptsCommands) return false

        platformStateDirty.set(true)
        signalDrain()
        layer.needRender()
        return true
    }

    private fun updatePlatformState(transform: (HiroComposePlatformState) -> HiroComposePlatformState): Boolean {
        while (true) {
            if (!state.get().acceptsCommands) return false

            val current = latestPlatformState.get()
            val next = transform(current)
            if (next == current) return true
            if (latestPlatformState.compareAndSet(current, next)) return signalPlatformStateChanged()
        }
    }

    private fun signalDrain() {
        if (!state.get().acceptsCommands || !drainScheduled.compareAndSet(false, true)) return
        if (!dispatcher.tryDispatchLater(Runnable(::drainCommands))) drainScheduled.set(false)
    }

    private fun drainCommands() {
        checkRenderThread()

        if (!state.get().acceptsCommands) {
            commands.clear()
            drainScheduled.set(false)
            return
        }
        
        val viewport = latestPlatformState.get().viewport
        if (viewport == null) {
            drainScheduled.set(false)
            return
        }

        applyLatestPlatformState()
        val batch = commands.takeSnapshot()
        try {
            for (command in batch) {
                if (terminated || !state.get().acceptsCommands) break

                when (command) {
                    is HiroComposeCommand.SetContent -> ensureScene().setContent(command.content)
                    is HiroComposeCommand.PointerEvent -> ensureScene().sendPointerEvent(command.event)
                    is HiroComposeCommand.MoveLifecycle -> ensureScene().moveLifecycleTo(command.state)
                    is HiroComposeCommand.ImeEdit -> ensureScene().performImeEdit(command.request)
                    is HiroComposeCommand.ImeAction -> ensureScene().performImeAction(command.sessionId, command.action)
                    is HiroComposeCommand.ImeKeyInput -> ensureScene().sendKeyEvent(command.event)
                    is HiroComposeCommand.ViewKeyInput -> if (command.request.claim()) {
                        try {
                            command.request.complete(ensureScene().sendKeyEvent(command.request.event))
                        } catch (throwable: Throwable) {
                            command.request.complete(true)
                            throw throwable
                        }
                    }
                    HiroComposeCommand.CancelPointerInput -> scene?.cancelPointerInput()
                    HiroComposeCommand.NavigationBack -> scene?.dispatchNavigationBack()
                    HiroComposeCommand.ReleaseFocus -> scene?.releaseFocus()
                    is HiroComposeCommand.TakeFocus -> ensureScene().takeFocus(command.direction)
                }
            }
        } finally {
            batch.forEach { command ->
                if (command is HiroComposeCommand.ViewKeyInput) command.request.cancel()
            }
            drainScheduled.set(false)
            if (commands.isNotEmpty()) signalDrain()
        }
    }

    private fun applyLatestPlatformState() {
        if (!platformStateDirty.compareAndSet(true, false)) return

        val latest = latestPlatformState.get()
        val viewport = latest.viewport ?: return
        ensureScene().apply {
            updateViewport(viewport)
            updateEnvironment(latest.environment)
            updateWindowInsets(latest.windowInsets)
            latest.inputMode?.let(::updateInputMode)
        }
    }

    private fun ensureScene(): HiroSkiaComposeScene {
        checkRenderThread()

        scene?.let { return it }
        check(!terminated) { "Hiro Compose 渲染控制器已经终止" }

        state.compareAndSet(HiroComposeRenderState.WaitingForRenderThread, HiroComposeRenderState.Running)
        val registration = HiroRenderDispatcherRegistry.register(dispatcher, androidPlatformServices)
        var createdScene: HiroSkiaComposeScene? = null

        try {
            return HiroSkiaComposeScene(
                scheduleFrame = layer::needRender,
                dispatcher = dispatcher,
                initialEnvironment = latestPlatformState.get().environment,
                requestInputMode = requestInputMode,
                requestNavigationBackHandling = requestNavigationBackHandling,
                savedStateTransport = savedStateTransport,
                savableStateConfiguration = savableStateConfiguration,
                imeHost = imeHost,
                focusBridge = focusBridge,
                androidPlatformServices = androidPlatformServices,
            ).also { nextScene ->
                createdScene = nextScene
                scene = nextScene
                dispatcherRegistration = registration
            }
        } catch (throwable: Throwable) {
            try {
                @Suppress("KotlinConstantConditions") // 在 also 里设了
                createdScene?.close()
            } catch (closeFailure: Throwable) {
                throwable.addSuppressed(closeFailure)
            }

            try {
                registration.close()
            } catch (closeFailure: Throwable) {
                throwable.addSuppressed(closeFailure)
            }

            throw throwable
        }
    }

    private fun checkRenderThread() = check(dispatcher.isOnRenderThread()) { "Hiro Compose 渲染控制器只能在 Skia 渲染线程操作" }

    companion object {
        private const val TAG = "HiroComposeRender"
        private const val VIEW_KEY_EVENT_TIMEOUT_MILLIS = 32L
    }
}

private enum class HiroComposeRenderState(val acceptsCommands: Boolean) { WaitingForRenderThread(true), Running(true), Closing(false), Closed(false) }
