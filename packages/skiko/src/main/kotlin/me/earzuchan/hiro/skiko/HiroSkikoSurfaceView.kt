package me.earzuchan.hiro.skiko

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Looper
import android.view.ViewGroup
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.Picture
import org.jetbrains.skia.PictureRecorder
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class HiroSkikoSurfaceView internal constructor(
    context: Context,
    private val layer: HiroSkiaLayer,
    private val config: HiroSkikoLayerConfig,
) : GLSurfaceView(context) {
    private val renderer = HiroSkikoSurfaceRenderer(layer, config, ::requestNextFrame)

    init {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        preserveEGLContextOnPause = config.preserveEglContextOnPause
        setEGLConfigChooser(HiroEglConfigChooser(config))
        setEGLContextClientVersion(config.eglContextClientVersion)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun scheduleFrame() {
        // TODO：接入 Compose 高频 invalidate 后，需要合并同一主线程循环里的重复帧请求
        if (Looper.myLooper() == Looper.getMainLooper()) {
            renderFrameOnMainThread()
        } else {
            post(::renderFrameOnMainThread)
        }
    }

    internal fun releaseRenderer() {
        queueEvent { renderer.release() }
    }

    private fun renderFrameOnMainThread() {
        // TODO：后续需要区分只需要重绘和需要重新录制 Picture 的场景
        renderer.update()
        requestRender()
    }

    private fun requestNextFrame() {
        // TODO：后续需要接入更严格的 vsync 节流和背压策略，避免动画连续请求时过度录制
        post(::scheduleFrame)
    }
}

private class HiroSkikoSurfaceRenderer(
    private val layer: HiroSkiaLayer,
    private val config: HiroSkikoLayerConfig,
    private val requestNextFrame: () -> Unit,
) : GLSurfaceView.Renderer {
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

    fun update() {
        if (released || width <= 0 || height <= 0) return

        // TODO：后续接 ComposeScene 后，需要评估是否可以减少 PictureRecorder 的每帧分配
        val delegate = layer.renderDelegate ?: return
        val recorder = PictureRecorder()
        val bounds = Rect.makeWH(width.toFloat(), height.toFloat())
        val recordingCanvas = recorder.beginRecording(bounds)

        try {
            delegate.onRender(recordingCanvas, width, height, System.nanoTime())
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
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        gl ?: return
        this.width = width
        this.height = height
        initCanvas(gl)
        requestNextFrame()
    }

    override fun onDrawFrame(gl: GL10?) {
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
        disposeCanvas()

        val framebufferId = queryGlInteger(gl, GLES20.GL_FRAMEBUFFER_BINDING)
        val actualSampleCount = queryGlInteger(gl, GLES20.GL_SAMPLES).coerceAtLeast(config.sampleCount)
        val actualStencilBits = queryGlInteger(gl, GLES20.GL_STENCIL_BITS).let {
            if (it > 0) it else config.stencilBufferBits
        }

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
    }

    private fun queryGlInteger(gl: GL10, name: Int): Int =
        IntBuffer.allocate(1).also {
            gl.glGetIntegerv(name, it)
        }[0]
}

private data class HiroPictureHolder(
    val picture: Picture,
    val width: Int,
    val height: Int,
)
