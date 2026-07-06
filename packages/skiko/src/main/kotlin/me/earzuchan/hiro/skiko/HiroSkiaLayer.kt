package me.earzuchan.hiro.skiko

import android.util.Log
import android.view.ViewGroup
import org.jetbrains.skia.Canvas

fun interface HiroSkiaRenderDelegate {
    fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long)
}

class HiroSkiaLayer(private val config: HiroSkikoLayerConfig = HiroSkikoLayerConfig()) : AutoCloseable {
    private var container: ViewGroup? = null

    var renderDelegate: HiroSkiaRenderDelegate? = null

    var surfaceView: HiroSkikoSurfaceView? = null; private set

    companion object{
        private const val TAG = "HiroSkiaLayer"
    }

    init {
        Log.d(TAG, "啊创建一个哦")
    }

    fun attachTo(mambaGroup: ViewGroup): HiroSkikoSurfaceView {
        Log.d(TAG, "啊将被贴附到啊一个$mambaGroup")

        detach()

        val view = HiroSkikoSurfaceView(context = mambaGroup.context, layer = this, config = config)
        mambaGroup.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        container = mambaGroup
        surfaceView = view

        needRender()
        return view
    }

    fun configureSurfaceView(action: HiroSkikoSurfaceView.() -> Unit) {
        Log.d(TAG, "啊将配置一个SurfaceView")

        surfaceView?.action()
    }

    fun needRender() = surfaceView?.scheduleFrame()

    fun onHostPause() {
        Log.d(TAG, "啊宿主暂停有感觉吗")

        surfaceView?.onPause()
    }

    fun onHostResume() {
        Log.d(TAG, "啊宿主恢复了呜呜呜")

        surfaceView?.onResume()

        needRender()
    }

    fun detach() {
        Log.d(TAG, "啊断舍离有感觉吗")

        val view = surfaceView ?: return

        view.releaseRenderer()
        container?.removeView(view)
        surfaceView = null
        container = null
    }

    override fun close() {
        Log.d(TAG, "啊被关闭了呢哥哥")

        detach()
    }
}
