package me.earzuchan.hiro.compose

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import me.earzuchan.hiro.skiko.HiroSkiaLayer
import me.earzuchan.hiro.skiko.HiroSkiaRenderDelegate

class HiroComposeView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr), AutoCloseable {
    private val layer = HiroSkiaLayer()
    private val scene = HiroSkikoComposeScene(scheduleFrame = { layer.needRender() }, density = currentDensity(), layoutDirection = currentLayoutDirection())
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

        if (layer.surfaceView == null) layer.attachTo(this)

        layer.onHostResume()
    }

    override fun onDetachedFromWindow() {
        Log.d(TAG, "啊要从一个啊一个窗口上脱离了")

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

        layer.needRender()
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(layoutDirection)

        Log.d(TAG, "啊布局方向变了啊一个：$layoutDirection")

        layer.needRender()
    }

    override fun close() {
        if (closed) return
        closed = true

        Log.d(TAG, "啊被关闭了一个（悲）")

        layer.renderDelegate = null
        layer.close()
        scene.close()
    }

    private fun currentDensity() = Density(density = resources.displayMetrics.density, fontScale = resources.configuration.fontScale)

    private fun currentLayoutDirection() = if (layoutDirection == LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr
}
