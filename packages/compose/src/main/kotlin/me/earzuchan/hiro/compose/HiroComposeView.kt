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

        Log.d(TAG, "啊被创建啊一个")
    }

    fun setContent(content: @Composable () -> Unit) {
        check(!closed) { "HiroComposeView 已经关闭，不能再设置 Compose 内容" }

        Log.d(TAG, "啊被设了内容，哥哥的大内容进来了")

        scene.setContent(content)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (closed) return

        Log.d(TAG, "啊被贴到了一个啊窗口上了")

        windowInsetsReader.attach(this)
        scene.attachHostView(this)

        if (layer.surfaceView == null) layer.attachTo(this)

        layer.onHostResume()
    }

    override fun onDetachedFromWindow() {
        Log.d(TAG, "啊要从一个啊一个窗口上脱离了")

        windowInsetsReader.detach(this)
        scene.detachHostView(this)
        close()

        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (closed) return

        Log.d(TAG, "啊窗口啊一个可见性变了：$visibility")

        if (visibility == VISIBLE) layer.onHostResume() else layer.onHostPause()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        Log.d(TAG, "啊配置变了啊一个得让画面再哦齁齁哦哦哦")

        windowInsetsReader.requestApplyInsets()
        layer.needRender()
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(layoutDirection)

        Log.d(TAG, "啊布局方向变了啊一个：$layoutDirection")

        layer.needRender()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (closed) return false

        if (event.actionMasked == MotionEvent.ACTION_DOWN) requestFocus()

        val handled = inputRouter.dispatchTouchEvent(event)
        if (handled) return true

        return super.dispatchTouchEvent(event)
    }

    // TODO：后续接入dispatchHoverEvent，真鼠标和触控笔悬停；dispatchGenericMotionEvent，虚拟触控板等玩意儿；dispatchKeyEvent，服务硬键盘、焦点和快捷键

    override fun close() {
        if (closed) return
        closed = true

        Log.d(TAG, "啊被关闭了一个（悲）")

        windowInsetsReader.close()
        layer.renderDelegate = null
        layer.close()
        scene.close()
    }

    private fun currentDensity() = Density(density = resources.displayMetrics.density, fontScale = resources.configuration.fontScale)

    private fun currentLayoutDirection() = if (layoutDirection == LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr
}
