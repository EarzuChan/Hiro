package me.earzuchan.hiro.skia

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Looper
import android.util.Log
import android.view.Choreographer
import android.view.ViewGroup
import org.jetbrains.skia.*
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@SuppressLint("ViewConstructor") // 不许在XML里用它
class HiroSkiaSurfaceView internal constructor(context: Context, layer: HiroSkiaLayer, config: HiroSkiaLayerConfig) : GLSurfaceView(context) {
    private val renderer = HiroSkiaSurfaceRenderer(layer, config, ::requestNextFrame)
    private val choreographer: Choreographer by lazy(LazyThreadSafetyMode.NONE) { Choreographer.getInstance() }
    private val frameCallback = Choreographer.FrameCallback(::doFrame)

    private var frameScheduled: Boolean = false
    private var frameRunning: Boolean = false
    private var frameRequestedDuringFrame: Boolean = false
    private var released: Boolean = false

    companion object{
        private const val TAG = "HiroSkiaSurfaceView"
    }

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        preserveEGLContextOnPause = config.preserveEglContextOnPause
        setEGLConfigChooser(HiroEglConfigChooser(config))
        setEGLContextClientVersion(config.eglContextClientVersion)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        isFocusable = true
        isFocusableInTouchMode = true

        Log.d(TAG, "啊被创建啊一个")
    }

    fun scheduleFrame() {
        if (Looper.myLooper() == Looper.getMainLooper()) scheduleFrameOnMainThread() else post(::scheduleFrameOnMainThread)
        // TIPS：不在这打LOG，太高频被调用
    }

    internal fun releaseRenderer() {
        Log.d(TAG, "啊释放啊一个渲染器")

        if (Looper.myLooper() == Looper.getMainLooper()) cancelFramePumpOnMainThread() else post(::cancelFramePumpOnMainThread)

        queueEvent { renderer.release() }
    }

    private fun scheduleFrameOnMainThread() {
        // TIPS：同上，不打LOG

        if (released) return

        if (frameRunning) {
            frameRequestedDuringFrame = true
            return
        }

        if (frameScheduled) return

        frameScheduled = true
        choreographer.postFrameCallback(frameCallback)
    }

    private fun cancelFramePumpOnMainThread() {
        released = true

        if (frameScheduled) choreographer.removeFrameCallback(frameCallback)

        frameScheduled = false
        frameRequestedDuringFrame = false
    }

    private fun doFrame(frameTimeNanos: Long) {
        if (released) return

        frameScheduled = false
        frameRunning = true
        frameRequestedDuringFrame = false

        try {
            renderFrameOnMainThread(frameTimeNanos)
        } finally {
            frameRunning = false
        }

        if (frameRequestedDuringFrame) scheduleFrameOnMainThread()
    }

    private fun renderFrameOnMainThread(frameTimeNanos: Long) {
        // TODO：后续需要区分只需要重绘和需要重新录制 Picture 的场景

        renderer.update(frameTimeNanos)
        requestRender()
    }

    private fun requestNextFrame() = scheduleFrame()
}

private class HiroSkiaSurfaceRenderer(private val layer: HiroSkiaLayer, private val config: HiroSkiaLayerConfig, private val requestNextFrame: () -> Unit) : GLSurfaceView.Renderer {
    private val pictureLock = Any()

    @Volatile
    private var width: Int = 0

    @Volatile
    private var height: Int = 0

    private var picture: HiroPictureHolder? = null
    private var directContext: DirectContext? = null
    private var renderTarget: BackendRenderTarget? = null
    private var surface: Surface? = null
    private var canvas: Canvas? = null

    @Volatile
    private var released: Boolean = false

    companion object{
        private const val TAG = "HiroSkiaSurfaceRenderer"
    }

    init {
        Log.d(TAG, "啊被创建啊一个")
    }

    fun update(frameTimeNanos: Long) {
        if (released || width <= 0 || height <= 0) return

        // TODO：后续接 ComposeScene 后，需要评估是否可以减少 PictureRecorder 的每帧分配
        val delegate = layer.renderDelegate ?: return
        val recorder = PictureRecorder()
        val bounds = Rect.makeWH(width.toFloat(), height.toFloat())
        val recordingCanvas = recorder.beginRecording(bounds)

        try {
            delegate.onRender(recordingCanvas, width, height, frameTimeNanos)
        } finally {
            val newPicture = recorder.finishRecordingAsPicture()

            val oldPicture = synchronized(pictureLock) {
                val old = picture
                picture = HiroPictureHolder(newPicture, width, height)
                old
            }

            oldPicture?.picture?.close()
        }
    }

    override fun onSurfaceCreated(gl: GL10?, eglConfig: EGLConfig?) {
        gl ?: return
        gl.glClearColor(0f, 0f, 0f, 0f)
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT)

        Log.d(TAG, "啊一个啊表面被创建")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        gl ?: return
        this.width = width
        this.height = height
        initCanvas(gl)
        requestNextFrame()

        Log.d(TAG, "啊一个啊表面被改变啊")
    }

    override fun onDrawFrame(gl: GL10?) {
        // TIPS：这个不打LOG

        gl ?: return

        gl.glClearColor(0f, 0f, 0f, 0f)
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT or GL10.GL_STENCIL_BUFFER_BIT)

        val holder = synchronized(pictureLock) { picture }
        val targetCanvas = canvas
        if (holder != null && targetCanvas != null) {
            targetCanvas.clear(config.clearColor)
            targetCanvas.drawPicture(holder.picture)
        }

        directContext?.flush()
    }

    fun release() {
        Log.d(TAG, "啊一个啊被释放")

        released = true
        val oldPicture = synchronized(pictureLock) {
            val old = picture
            picture = null
            old
        }
        oldPicture?.picture?.close()
        disposeCanvas()
    }

    private fun initCanvas(gl: GL10) {
        Log.d(TAG, "啊想初始化啊一个画布")

        disposeCanvas()

        val framebufferId = queryGlInteger(gl, GLES20.GL_FRAMEBUFFER_BINDING)
        val actualSampleCount = queryGlInteger(gl, GLES20.GL_SAMPLES).coerceAtLeast(config.sampleCount)
        val actualStencilBits = queryGlInteger(gl, GLES20.GL_STENCIL_BITS).let { if (it > 0) it else config.stencilBufferBits }

        renderTarget = BackendRenderTarget.makeGL(
            width,
            height,
            actualSampleCount,
            actualStencilBits,
            framebufferId,
            FramebufferFormat.GR_GL_RGBA8,
        )
        directContext = DirectContext.makeGL()
        surface = Surface.makeFromBackendRenderTarget(
            directContext!!,
            renderTarget!!,
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.sRGB,
        ) ?: error("无法创建 Skia GPU Surface")
        canvas = surface?.canvas
    }

    private fun disposeCanvas() {
        canvas = null
        surface?.close()
        renderTarget?.close()
        directContext?.close()
        surface = null
        renderTarget = null
        directContext = null

        Log.d(TAG, "啊一个啊画布被处理")
    }

    private fun queryGlInteger(gl: GL10, name: Int) = IntBuffer.allocate(1).also { gl.glGetIntegerv(name, it) }[0]
}

private data class HiroPictureHolder(val picture: Picture, val width: Int, val height: Int)
