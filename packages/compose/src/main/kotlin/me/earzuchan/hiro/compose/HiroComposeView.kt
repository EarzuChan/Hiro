package me.earzuchan.hiro.compose

import android.content.Context
import android.content.res.Configuration
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import me.earzuchan.hiro.compose.internal.HiroComposeEnvironment
import me.earzuchan.hiro.compose.internal.HiroComposeRenderController
import me.earzuchan.hiro.compose.internal.architecture.HiroAndroidHostBridge
import me.earzuchan.hiro.compose.internal.architecture.HiroSavedStateTransport
import me.earzuchan.hiro.compose.internal.input.HiroAndroidInputModeFiddler
import me.earzuchan.hiro.compose.internal.input.HiroAndroidInputRouter
import me.earzuchan.hiro.compose.internal.input.HiroComposeInputSink
import me.earzuchan.hiro.compose.internal.input.HiroComposePointerEvent
import me.earzuchan.hiro.compose.internal.windowinsets.HiroWindowInsetsFiddlerForAndroid
import me.earzuchan.hiro.skia.HiroSkiaLayer

class HiroComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), AutoCloseable {
    private val layer = HiroSkiaLayer()
    private val savedStateTransport = HiroSavedStateTransport()
    private val renderController = HiroComposeRenderController(
        layer = layer,
        initialEnvironment = currentEnvironment(),
        requestInputMode = ::requestInputModeFromRenderThread,
        requestNavigationBackHandling = ::requestNavigationBackHandlingFromRenderThread,
        savedStateTransport = savedStateTransport,
    )
    private val hostBridge = HiroAndroidHostBridge(
        savedStateTransport = savedStateTransport,
        onLifecycleChanged = ::synchronizeRenderLifecycle,
        onNavigationBack = renderController::dispatchNavigationBack,
    )
    private val windowInsetsReader = HiroWindowInsetsFiddlerForAndroid(renderController::updateWindowInsets)
    private val inputModeReader = HiroAndroidInputModeFiddler(renderController::updateInputMode)
    private val inputRouter = HiroAndroidInputRouter(object : HiroComposeInputSink {
        override fun sendPointerEvent(event: HiroComposePointerEvent) = renderController.sendPointerEvent(event)

        override fun cancelPointerInput() {
            renderController.cancelPointerInput()
        }
    })

    @Volatile
    private var closed = false

    private var renderActive: Boolean? = null

    companion object {
        private const val TAG = "HiroComposeView"
    }

    init {
        if (id == NO_ID) id = R.id.hiro_compose_view

        layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        isFocusable = true
        isFocusableInTouchMode = true
        layer.renderDelegate = renderController

        Log.d(TAG, "被创建")
    }

    fun setContent(content: @Composable () -> Unit) {
        checkMainThread()
        check(!closed) { "HiroComposeView 已经关闭，不能再设置 Compose 内容" }
        check(renderController.setContent(content)) { "Hiro Compose 内容无法投递到渲染线程" }

        Log.d(TAG, "已接收 Compose 内容")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        checkMainThread()
        if (closed) return

        hostBridge.attach(this)
        renderController.updateEnvironment(currentEnvironment())
        windowInsetsReader.attach(this)
        inputModeReader.attach(this)

        if (layer.surfaceView == null) {
            layer.attachTo(this)
            renderController.wake()
        }

        synchronizeRenderLifecycle(force = true)
        Log.d(TAG, "已挂载到窗口")
    }

    override fun onDetachedFromWindow() {
        checkMainThread()

        Log.d(TAG, "将从窗口脱离")
        close()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (!closed) synchronizeRenderLifecycle()
    }

    override fun onVisibilityChanged(changedView: android.view.View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (!closed) synchronizeRenderLifecycle()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        if (closed) return
        renderController.updateEnvironment(currentEnvironment())
        windowInsetsReader.requestApplyInsets()
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(layoutDirection)
        if (!closed) renderController.updateEnvironment(currentEnvironment())
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (closed) return false
        if (event.actionMasked == MotionEvent.ACTION_DOWN) requestFocus()

        val handled = inputRouter.dispatchTouchEvent(event)
        if (event.shouldLogTouchEvent()) Log.d(TAG, "触摸事件：${event.name()}，指针数：${event.pointerCount}，动作指针：${event.actionIndex}，已处理：$handled")

        return handled || super.dispatchTouchEvent(event)
    }

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
        }

        Log.d(TAG, "已完整关闭")
    }

    private fun synchronizeRenderLifecycle(force: Boolean = false) {
        checkMainThread()
        if (closed || layer.surfaceView == null) return

        val hostState = hostBridge.lifecycleState
        if (hostState == Lifecycle.State.DESTROYED) {
            close()
            return
        }

        val viewActive = isAttachedToWindow && windowVisibility == VISIBLE && isShown
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
    else post { if (!closed) inputModeReader.request(inputMode) }

    // TIPS：这里是最神秘的“申请NavBack”
    private fun requestNavigationBackHandlingFromRenderThread(enabled: Boolean): Boolean = if (closed) false
    else post { if (!closed) hostBridge.updateNavigationBackHandling(enabled) }

    private fun currentEnvironment() = HiroComposeEnvironment(
        density = Density(resources.displayMetrics.density, resources.configuration.fontScale),
        layoutDirection = if (layoutDirection == LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr,
        systemTheme = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> SystemTheme.Dark
            Configuration.UI_MODE_NIGHT_NO -> SystemTheme.Light
            else -> SystemTheme.Unknown
        }
    )

    private fun checkMainThread() = check(Looper.myLooper() == Looper.getMainLooper()) { "HiroComposeView 只能在安卓主线程操作" }
}

private fun MotionEvent.shouldLogTouchEvent() = when (actionMasked) {
    MotionEvent.ACTION_DOWN,
    MotionEvent.ACTION_UP,
    MotionEvent.ACTION_POINTER_DOWN,
    MotionEvent.ACTION_POINTER_UP,
    MotionEvent.ACTION_CANCEL,
    MotionEvent.ACTION_OUTSIDE -> true
    else -> false
}

private fun MotionEvent.name() = when (actionMasked) {
    MotionEvent.ACTION_DOWN -> "按下"
    MotionEvent.ACTION_UP -> "抬起"
    MotionEvent.ACTION_POINTER_DOWN -> "多指按下"
    MotionEvent.ACTION_POINTER_UP -> "多指抬起"
    MotionEvent.ACTION_CANCEL -> "取消"
    MotionEvent.ACTION_OUTSIDE -> "越界"
    else -> "其他"
}
