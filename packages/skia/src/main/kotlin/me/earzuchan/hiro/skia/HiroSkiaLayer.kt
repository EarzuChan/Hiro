package me.earzuchan.hiro.skia

import android.util.Log
import android.view.ViewGroup
import org.jetbrains.skia.Canvas

fun interface HiroSkiaRenderDelegate {
    fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long)
}

class HiroSkiaLayer(private val config: HiroSkiaLayerConfig = HiroSkiaLayerConfig()) : AutoCloseable {
    private var container: ViewGroup? = null

    var renderDelegate: HiroSkiaRenderDelegate? = null

    var surfaceView: HiroSkiaSurfaceView? = null; private set

    companion object{
        private const val TAG = "HiroSkiaLayer"
    }

    init {
        Log.d(TAG, "被创建")
    }

    fun attachTo(mambaGroup: ViewGroup): HiroSkiaSurfaceView {
        Log.d(TAG, "将被贴附到：$mambaGroup")

        detach()

        val view = HiroSkiaSurfaceView(context = mambaGroup.context, layer = this, config = config)
        mambaGroup.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        container = mambaGroup
        surfaceView = view

        needRender()
        return view
    }

    fun configureSurfaceView(action: HiroSkiaSurfaceView.() -> Unit) {
        Log.d(TAG, "将配置SurfaceView")

        surfaceView?.action()
    }

    fun needRender() = surfaceView?.scheduleFrame()

    fun onHostPause() {
        Log.d(TAG, "宿主被暂停")

        surfaceView?.onPause()
    }

    fun onHostResume() {
        Log.d(TAG, "宿主被恢复")

        surfaceView?.onResume()

        needRender()
    }

    fun detach() {
        Log.d(TAG, "将脱离SurfaceView")

        val view = surfaceView ?: return

        view.releaseRenderer()
        container?.removeView(view)
        surfaceView = null
        container = null
    }

    override fun close() {
        Log.d(TAG, "被关闭")

        detach()
    }
}
