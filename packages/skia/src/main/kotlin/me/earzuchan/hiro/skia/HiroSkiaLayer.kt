package me.earzuchan.hiro.skia

import android.util.Log
import android.view.ViewGroup
import me.earzuchan.hiro.skia.util.checkHiroMainThread
import org.jetbrains.skia.Canvas

fun interface HiroSkiaRenderDelegate {
    fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long)
}

interface HiroSkiaRenderLifecycleDelegate {
    fun onRenderThreadClosing()
}

class HiroSkiaLayer(private val config: HiroSkiaLayerConfig = HiroSkiaLayerConfig()) : AutoCloseable {
    private var container: ViewGroup? = null

    @Volatile
    var renderDelegate: HiroSkiaRenderDelegate? = null

    var surfaceView: HiroSkiaSurfaceView? = null; private set

    companion object {
        private const val TAG = "HiroSkiaLayer"
    }

    fun attachTo(container: ViewGroup): HiroSkiaSurfaceView {
        checkHiroMainThread()
        check(surfaceView == null) { "HiroSkiaLayer 已经挂载" }

        val view = HiroSkiaSurfaceView(container.context, this, config)
        container.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        this.container = container
        surfaceView = view
        needRender()
        Log.d(TAG, "已挂载到宿主")
        return view
    }

    fun configureSurfaceView(action: HiroSkiaSurfaceView.() -> Unit) {
        checkHiroMainThread()
        surfaceView?.action()
    }

    fun postToRenderThread(block: Runnable): Boolean = surfaceView?.postToRenderThread(block) == true

    fun queueToRenderThread(block: Runnable): Boolean = surfaceView?.queueToRenderThread(block) == true

    fun isOnRenderThread(): Boolean = surfaceView?.isOnRenderThread() == true

    fun needRender() = surfaceView?.scheduleFrame()

    fun onHostPause(beforePause: Runnable = Runnable {}) {
        checkHiroMainThread()
        surfaceView?.pauseRenderThread(beforePause)
    }

    fun onHostResume(afterResume: Runnable = Runnable {}) {
        checkHiroMainThread()
        surfaceView?.resumeRenderThread(afterResume)
    }

    fun detach() {
        checkHiroMainThread()

        val view = surfaceView ?: return
        try {
            view.releaseRenderer()
        } finally {
            try {
                container?.removeView(view)
            } finally {
                surfaceView = null
                container = null
                renderDelegate = null
            }
        }

        Log.d(TAG, "已从宿主脱离")
    }

    override fun close() = detach()
}
