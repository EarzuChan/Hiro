package me.earzuchan.hiro.compose.internal

import android.os.Looper
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import me.earzuchan.hiro.compose.HiroComposeConfiguration
import me.earzuchan.hiro.compose.HiroComposeView
import me.earzuchan.hiro.compose.internal.architecture.HiroAndroidHostBridge
import me.earzuchan.hiro.compose.internal.architecture.HiroSavedStateTransport
import me.earzuchan.hiro.compose.internal.input.HiroAndroidInputModeFiddler
import me.earzuchan.hiro.compose.internal.input.HiroAndroidInputRouter
import me.earzuchan.hiro.compose.internal.input.HiroComposeInputSink
import me.earzuchan.hiro.compose.internal.input.HiroComposePointerEvent
import me.earzuchan.hiro.compose.internal.windowinsets.HiroWindowInsetsFiddlerForAndroid
import me.earzuchan.hiro.skia.HiroSkiaLayer

internal class HiroComposeHostSession(private val view: HiroComposeView, private val configuration: HiroComposeConfiguration, savedStateKey: String?, savedStateTransport: HiroSavedStateTransport, private val onHostDestroyed: () -> Unit) : AutoCloseable {
    private val environmentReader = HiroComposeEnvironmentReader(view, configuration)

    private val layer = HiroSkiaLayer()

    private val renderController = HiroComposeRenderController(
        layer = layer,
        initialEnvironment = environmentReader.read(),
        initialWindowInsets = configuration.environment.windowInsetsPolicy.initial,
        requestInputMode = ::requestInputModeFromRenderThread,
        requestNavigationBackHandling = ::requestNavigationBackHandlingFromRenderThread,
        savedStateTransport = savedStateTransport,
        savableStateConfiguration = configuration.savableStateConfiguration,
    )

    private val hostBridge = HiroAndroidHostBridge(
        savedStateTransport = savedStateTransport,
        savedStateKey = savedStateKey,
        onLifecycleChanged = ::onHostLifecycleChanged,
        onNavigationBack = renderController::dispatchNavigationBack,
    )

    private val windowInsetsReader = HiroWindowInsetsFiddlerForAndroid { system -> renderController.updateWindowInsets(configuration.environment.windowInsetsPolicy.resolve(system)) }

    private val inputModeReader = HiroAndroidInputModeFiddler(renderController::updateInputMode)

    private val inputRouter = HiroAndroidInputRouter(object : HiroComposeInputSink {
        override fun sendPointerEvent(event: HiroComposePointerEvent) = renderController.sendPointerEvent(event)

        override fun cancelPointerInput() {
            renderController.cancelPointerInput()
        }
    })

    private var attached = false
    private var closed = false
    private var renderActive: Boolean? = null

    init {
        checkMainThread()
        layer.renderDelegate = renderController
    }

    fun attach() {
        checkMainThread()
        check(!closed) { "Hiro Compose 宿主会话已经关闭" }
        check(!attached) { "Hiro Compose 宿主会话已经挂载" }
        attached = true

        hostBridge.attach(view)
        if (closed) return

        renderController.updateEnvironment(environmentReader.read())
        if (configuration.environment.windowInsetsPolicy.readsSystem) windowInsetsReader.attach(view)
        inputModeReader.attach(view)
        layer.attachTo(view)
        if (view.width > 0 && view.height > 0) renderController.updateViewport(IntSize(view.width, view.height))
        renderController.wake()
        synchronizeLifecycle(force = true)
    }

    fun setContent(content: @Composable () -> Unit) {
        checkMainThread()
        check(!closed) { "Hiro Compose 宿主会话已经关闭" }
        check(renderController.setContent(content)) { "Hiro Compose 内容无法投递到渲染线程" }
    }

    fun updateEnvironment() {
        checkMainThread()
        if (closed) return

        renderController.updateEnvironment(environmentReader.read())
        if (configuration.environment.windowInsetsPolicy.readsSystem) windowInsetsReader.requestApplyInsets()
    }

    fun updateViewport(width: Int, height: Int) {
        if (!closed && width > 0 && height > 0) renderController.updateViewport(IntSize(width, height))
    }

    fun synchronizeLifecycle() = synchronizeLifecycle(force = false)

    fun dispatchTouchEvent(event: MotionEvent): Boolean = if (closed) false else inputRouter.dispatchTouchEvent(event)

    override fun close() {
        checkMainThread()
        if (closed) return
        closed = true

        windowInsetsReader.close()
        inputModeReader.close()
        renderController.beginClose()

        try {
            if (layer.surfaceView == null) renderController.closeBeforeRenderThreadStarts() else layer.close()
        } finally {
            hostBridge.close()
            renderActive = null
            attached = false
        }
    }

    private fun onHostLifecycleChanged() {
        if (hostBridge.lifecycleState == Lifecycle.State.RESUMED) updateEnvironment()
        synchronizeLifecycle(force = false)
    }

    private fun synchronizeLifecycle(force: Boolean) {
        checkMainThread()
        if (closed || layer.surfaceView == null) return

        val hostState = hostBridge.lifecycleState
        if (hostState == Lifecycle.State.DESTROYED) {
            onHostDestroyed()
            return
        }

        val viewActive = view.isAttachedToWindow && view.windowVisibility == android.view.View.VISIBLE && view.isShown
        val targetState = when {
            hostState == Lifecycle.State.RESUMED && viewActive -> Lifecycle.State.RESUMED
            hostState.isAtLeast(Lifecycle.State.STARTED) -> Lifecycle.State.STARTED
            else -> Lifecycle.State.CREATED
        }
        renderController.updateLifecycle(targetState)

        val shouldRun = targetState == Lifecycle.State.RESUMED
        if (!force && renderActive == shouldRun) return
        renderActive = shouldRun

        if (shouldRun) layer.onHostResume(Runnable(renderController::onHostResumeOnRenderThread))
        else layer.onHostPause(Runnable(renderController::onHostPauseOnRenderThread))
    }

    private fun requestInputModeFromRenderThread(inputMode: InputMode): Boolean = if (closed) false
    else view.post { if (!closed) inputModeReader.request(inputMode) }

    private fun requestNavigationBackHandlingFromRenderThread(enabled: Boolean): Boolean = if (closed) false
    else view.post { if (!closed) hostBridge.updateNavigationBackHandling(enabled) }

    private fun checkMainThread() = check(Looper.myLooper() == Looper.getMainLooper()) { "Hiro Compose 宿主会话只能在安卓主线程操作" }
}
