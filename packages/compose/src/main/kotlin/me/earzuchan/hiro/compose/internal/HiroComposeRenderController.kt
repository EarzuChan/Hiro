package me.earzuchan.hiro.compose.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import me.earzuchan.hiro.compose.HiroSkiaComposeScene
import me.earzuchan.hiro.compose.internal.architecture.HiroSavedStateTransport
import me.earzuchan.hiro.compose.internal.input.HiroComposePointerEvent
import me.earzuchan.hiro.compose.internal.windowinsets.HiroPlatformWindowInsetsSnapshot
import me.earzuchan.hiro.skia.HiroSkiaLayer
import me.earzuchan.hiro.skia.HiroSkiaRenderDelegate
import me.earzuchan.hiro.skia.HiroSkiaRenderLifecycleDelegate
import org.jetbrains.skia.Canvas
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal data class HiroComposeEnvironment(val density: Density, val layoutDirection: LayoutDirection, val systemTheme: SystemTheme)

internal class HiroComposeRenderController(private val layer: HiroSkiaLayer, initialEnvironment: HiroComposeEnvironment, private val requestInputMode: (InputMode) -> Boolean, private val requestNavigationBackHandling: (Boolean) -> Boolean, private val savedStateTransport: HiroSavedStateTransport) : HiroSkiaRenderDelegate, HiroSkiaRenderLifecycleDelegate {
    private val commands = ConcurrentLinkedQueue<HiroComposeCommand>()
    private val drainScheduled = AtomicBoolean(false)
    private val state = AtomicReference(HiroComposeRenderState.WaitingForRenderThread)
    private val latestEnvironment = AtomicReference(initialEnvironment)
    private val latestWindowInsets = AtomicReference<HiroPlatformWindowInsetsSnapshot?>(null)
    private val latestInputMode = AtomicReference<InputMode?>(null)
    private val dispatcher = HiroSkiaRenderDispatcher(layer::isOnRenderThread, layer::postToRenderThread, layer::queueToRenderThread)

    private var scene: HiroSkiaComposeScene? = null
    private var dispatcherRegistration: AutoCloseable? = null
    private var terminated = false

    fun setContent(content: @Composable () -> Unit): Boolean = post(HiroComposeCommand.SetContent(content))

    fun updateEnvironment(environment: HiroComposeEnvironment): Boolean {
        latestEnvironment.set(environment)
        return post(HiroComposeCommand.ApplyEnvironment)
    }

    fun updateWindowInsets(snapshot: HiroPlatformWindowInsetsSnapshot): Boolean {
        latestWindowInsets.set(snapshot)
        return post(HiroComposeCommand.ApplyWindowInsets)
    }

    fun updateInputMode(inputMode: InputMode): Boolean {
        latestInputMode.set(inputMode)
        return post(HiroComposeCommand.ApplyInputMode)
    }

    fun updateLifecycle(state: Lifecycle.State): Boolean = post(HiroComposeCommand.MoveLifecycle(state))

    fun sendPointerEvent(event: HiroComposePointerEvent): Boolean = post(HiroComposeCommand.PointerEvent(event))

    fun cancelPointerInput() = post(HiroComposeCommand.CancelPointerInput)

    fun dispatchNavigationBack(): Boolean = post(HiroComposeCommand.NavigationBack)

    fun wake() {
        signalDrain()
        layer.needRender()
    }

    fun onHostResumeOnRenderThread() {
        checkRenderThread()

        if (!state.get().acceptsCommands) return
        drainCommands()
        if (!state.get().acceptsCommands) return
        ensureScene()
    }

    fun onHostPauseOnRenderThread() {
        checkRenderThread()

        if (!state.get().acceptsCommands) return
        drainCommands()
        if (!state.get().acceptsCommands) return
        scene?.checkpointSavedState()
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
        dispatcher.close()
        terminated = true
        state.set(HiroComposeRenderState.Closed)
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        checkRenderThread()

        if (terminated || state.get() == HiroComposeRenderState.Closing) return
        drainCommands()
        scene?.render(canvas, width, height, nanoTime)
    }

    override fun onRenderThreadClosing() {
        checkRenderThread()
        if (terminated) return

        state.set(HiroComposeRenderState.Closing)
        commands.clear()
        var failure: Throwable? = null

        try {
            scene?.close()
        } catch (throwable: Throwable) {
            failure = throwable
        }

        scene = null
        try {
            dispatcherRegistration?.close()
        } catch (throwable: Throwable) {
            failure?.addSuppressed(throwable) ?: run { failure = throwable }
        }

        dispatcherRegistration = null
        dispatcher.close()
        terminated = true
        state.set(HiroComposeRenderState.Closed)
        failure?.let { throw it }
    }

    private fun post(command: HiroComposeCommand): Boolean {
        if (!state.get().acceptsCommands) return false

        commands.add(command)
        signalDrain()
        layer.needRender()
        return true
    }

    private fun signalDrain() {
        if (!state.get().acceptsCommands || !drainScheduled.compareAndSet(false, true)) return
        if (!layer.postToRenderThread(Runnable(::drainCommands))) drainScheduled.set(false)
    }

    private fun drainCommands() {
        checkRenderThread()

        drainScheduled.set(false)
        if (!state.get().acceptsCommands) {
            commands.clear()
            return
        }

        while (!terminated && state.get().acceptsCommands) when (val command = commands.poll() ?: break) {
            is HiroComposeCommand.SetContent -> ensureScene().setContent(command.content)
            HiroComposeCommand.ApplyEnvironment -> ensureScene().updateEnvironment(latestEnvironment.get())
            HiroComposeCommand.ApplyWindowInsets -> latestWindowInsets.get()?.let(ensureScene()::updateWindowInsets)
            HiroComposeCommand.ApplyInputMode -> latestInputMode.get()?.let(ensureScene()::updateInputMode)
            is HiroComposeCommand.PointerEvent -> ensureScene().sendPointerEvent(command.event)
            is HiroComposeCommand.MoveLifecycle -> ensureScene().moveLifecycleTo(command.state)
            HiroComposeCommand.CancelPointerInput -> scene?.cancelPointerInput()
            HiroComposeCommand.NavigationBack -> scene?.dispatchNavigationBack()
        }

        if (!state.get().acceptsCommands) commands.clear() else if (commands.isNotEmpty()) signalDrain()
    }

    private fun ensureScene(): HiroSkiaComposeScene {
        checkRenderThread()

        scene?.let { return it }
        check(!terminated) { "Hiro Compose 渲染控制器已经终止" }

        state.compareAndSet(HiroComposeRenderState.WaitingForRenderThread, HiroComposeRenderState.Running)
        val registration = HiroRenderDispatcherRegistry.register(dispatcher)
        var createdScene: HiroSkiaComposeScene? = null

        try {
            return HiroSkiaComposeScene(
                scheduleFrame = layer::needRender,
                dispatcher = dispatcher,
                initialEnvironment = latestEnvironment.get(),
                requestInputMode = requestInputMode,
                requestNavigationBackHandling = requestNavigationBackHandling,
                savedStateTransport = savedStateTransport,
            ).also { nextScene ->
                createdScene = nextScene
                latestWindowInsets.get()?.let(nextScene::updateWindowInsets)
                latestInputMode.get()?.let(nextScene::updateInputMode)
                scene = nextScene
                dispatcherRegistration = registration
            }
        } catch (throwable: Throwable) {
            try {
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
}

private enum class HiroComposeRenderState(val acceptsCommands: Boolean) { WaitingForRenderThread(true), Running(true), Closing(false), Closed(false) }

// TODO：六六六还有命令模式。看看要不要优化掉
private sealed interface HiroComposeCommand {
    data class SetContent(val content: @Composable () -> Unit) : HiroComposeCommand
    data object ApplyEnvironment : HiroComposeCommand
    data object ApplyWindowInsets : HiroComposeCommand
    data object ApplyInputMode : HiroComposeCommand
    data class PointerEvent(val event: HiroComposePointerEvent) : HiroComposeCommand
    data class MoveLifecycle(val state: Lifecycle.State) : HiroComposeCommand
    data object CancelPointerInput : HiroComposeCommand
    data object NavigationBack : HiroComposeCommand
}
