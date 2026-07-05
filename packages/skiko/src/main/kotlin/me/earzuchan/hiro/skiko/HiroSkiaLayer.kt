package me.earzuchan.hiro.skiko

import android.view.ViewGroup
import org.jetbrains.skia.Canvas

fun interface HiroSkiaRenderDelegate {
    fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long)
}

class HiroSkiaLayer(
    private val config: HiroSkikoLayerConfig = HiroSkikoLayerConfig(),
) : AutoCloseable {
    private var container: ViewGroup? = null

    var renderDelegate: HiroSkiaRenderDelegate? = null

    var surfaceView: HiroSkikoSurfaceView? = null
        private set

    fun attachTo(container: ViewGroup): HiroSkikoSurfaceView {
        detach()

        val view = HiroSkikoSurfaceView(
            context = container.context,
            layer = this,
            config = config,
        )
        container.addView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        this.container = container
        surfaceView = view
        needRender()
        return view
    }

    fun configureSurfaceView(action: HiroSkikoSurfaceView.() -> Unit) {
        surfaceView?.action()
    }

    fun needRender() {
        surfaceView?.scheduleFrame()
    }

    fun onHostPause() {
        surfaceView?.onPause()
    }

    fun onHostResume() {
        surfaceView?.onResume()
        needRender()
    }

    fun detach() {
        val view = surfaceView ?: return
        view.releaseRenderer()
        container?.removeView(view)
        surfaceView = null
        container = null
    }

    override fun close() {
        detach()
    }
}
