package me.earzuchan.hiro.compose

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import me.earzuchan.hiro.compose.internal.input.HiroAndroidInputRouter
import me.earzuchan.hiro.compose.internal.input.HiroComposeInputSink
import me.earzuchan.hiro.compose.internal.input.HiroComposePointerEvent
import me.earzuchan.hiro.compose.internal.windowinsets.HiroWindowInsetsFiddlerForAndroid
import me.earzuchan.hiro.compose.internal.windowinsets.HiroMutablePlatformWindowInsets
import me.earzuchan.hiro.skia.HiroSkiaLayer
import me.earzuchan.hiro.skia.HiroSkiaRenderDelegate

class HiroComposeView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr), AutoCloseable {
    private val layer = HiroSkiaLayer()

    private val windowInsets = HiroMutablePlatformWindowInsets()

    private val scene = HiroSkiaComposeScene(scheduleFrame = { layer.needRender() }, density = currentDensity(), layoutDirection = currentLayoutDirection(), windowInsets = windowInsets)

    private val windowInsetsReader = HiroWindowInsetsFiddlerForAndroid(windowInsets) { layer.needRender() }

    private val inputRouter = HiroAndroidInputRouter(object : HiroComposeInputSink {
        override fun sendPointerEvent(event: HiroComposePointerEvent): Boolean = scene.sendPointerEvent(event)

        override fun cancelPointerInput() = scene.cancelPointerInput()
    })

    private var closed = false

    companion object {
        private const val TAG = "HiroComposeView"
    }

    init {
        layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        isFocusable = true
        isFocusableInTouchMode = true

        layer.renderDelegate = HiroSkiaRenderDelegate { canvas, width, height, nanoTime -> scene.render(canvas = canvas, width = width, height = height, density = currentDensity(), layoutDirection = currentLayoutDirection(), nanoTime = nanoTime) }

        Log.d(TAG, "被创建")
    }

    fun setContent(content: @Composable () -> Unit) {
        check(!closed) { "HiroComposeView 已经关闭，不能再设置 Compose 内容" }

        Log.d(TAG, "被设了内容")

        scene.setContent(content)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (closed) return

        Log.d(TAG, "被贴到了窗口上")

        windowInsetsReader.attach(this)
        scene.attachHostView(this)

        if (layer.surfaceView == null) layer.attachTo(this)

        layer.onHostResume()
    }

    override fun onDetachedFromWindow() {
        Log.d(TAG, "将从窗口上脱离")

        windowInsetsReader.detach(this)
        scene.detachHostView(this)
        close()

        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (closed) return

        Log.d(TAG, "窗口的可见性变了：$visibility")

        if (visibility == VISIBLE) layer.onHostResume() else layer.onHostPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        Log.d(TAG, "配置变了，得重绘")

        windowInsetsReader.requestApplyInsets()
        layer.needRender()
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(layoutDirection)

        Log.d(TAG, "布局方向改变：$layoutDirection，得重绘")

        layer.needRender()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (closed) return false

        if (event.actionMasked == MotionEvent.ACTION_DOWN) requestFocus()

        val handled = inputRouter.dispatchTouchEvent(event)
        if (event.shouldLogTouchEvent()) Log.d(TAG, "触摸事件：${event.name()}，指针数：${event.pointerCount}，动作指针：${event.actionIndex}，已处理：$handled")

        if (handled) return true

        return super.dispatchTouchEvent(event)
    }

    // TODO：后续接入dispatchHoverEvent，真鼠标和触控笔悬停；dispatchGenericMotionEvent，虚拟触控板等玩意儿；dispatchKeyEvent，服务硬键盘、焦点和快捷键

    override fun close() {
        if (closed) return
        closed = true

        Log.d(TAG, "被关闭")

        windowInsetsReader.close()
        layer.renderDelegate = null
        layer.close()
        scene.close()
    }

    private fun currentDensity() = Density(density = resources.displayMetrics.density, fontScale = resources.configuration.fontScale)

    private fun currentLayoutDirection() = if (layoutDirection == LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr

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