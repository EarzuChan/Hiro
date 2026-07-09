package me.earzuchan.hiro.skia

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.EGL14
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Looper
import android.util.Log
import android.view.Choreographer
import android.view.ViewGroup
import me.earzuchan.hiro.skia.util.checkHiroMainThread
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import java.nio.IntBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@SuppressLint("ViewConstructor")
class HiroSkiaSurfaceView internal constructor(
    context: Context,
    private val layer: HiroSkiaLayer,
    config: HiroSkiaLayerConfig,
) : GLSurfaceView(context) {
    private val frameWanted = AtomicBoolean(false)
    private val frameTimeNanos = AtomicLong(0L)
    private val released = AtomicBoolean(false)
    private val renderer = HiroSkiaSurfaceRenderer(layer, config, ::consumeFrameTime)
    private val choreographer: Choreographer by lazy(LazyThreadSafetyMode.NONE) { Choreographer.getInstance() }
    private val frameCallback = Choreographer.FrameCallback(::doFrame)

    private var frameScheduled = false
    private var hostState = HiroSkiaHostState.Running
    private var attachedToPlatform = false
    private var glThreadStopped = false

    companion object {
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
        Log.d(TAG, "被创建")
    }

    fun postToRenderThread(block: Runnable): Boolean {
        if (released.get()) return false
        if (renderer.isOnRenderThread()) {
            renderer.runOnRenderThread(block)
        } else {
            queueEvent { renderer.runOnRenderThread(block) }
        }
        return true
    }

    fun queueToRenderThread(block: Runnable): Boolean {
        if (released.get()) return false
        queueEvent { renderer.runOnRenderThread(block) }
        return true
    }

    fun isOnRenderThread(): Boolean = renderer.isOnRenderThread()

    fun scheduleFrame() {
        if (released.get() || !frameWanted.compareAndSet(false, true)) return
        if (Looper.myLooper() == Looper.getMainLooper()) scheduleFrameOnMainThread() else post(::scheduleFrameOnMainThread)
    }

    internal fun pauseRenderThread(beforePause: Runnable) {
        checkHiroMainThread()
        if (hostState == HiroSkiaHostState.Closing || hostState == HiroSkiaHostState.Closed) return

        cancelFrameCallbackOnMainThread()
        runOnRenderThreadBlocking(beforePause)
        if (hostState != HiroSkiaHostState.Paused) {
            onPause()
            hostState = HiroSkiaHostState.Paused
        }
        Log.d(TAG, "渲染线程已暂停")
    }

    internal fun resumeRenderThread(afterResume: Runnable) {
        checkHiroMainThread()
        if (hostState == HiroSkiaHostState.Closing || hostState == HiroSkiaHostState.Closed) return

        if (hostState == HiroSkiaHostState.Paused) onResume()

        hostState = HiroSkiaHostState.Running
        postToRenderThread(afterResume)
        if (frameWanted.get()) scheduleFrameOnMainThread() else scheduleFrame()
        Log.d(TAG, "渲染线程已恢复")
    }

    internal fun releaseRenderer() {
        checkHiroMainThread()
        if (!released.compareAndSet(false, true)) return

        hostState = HiroSkiaHostState.Closing
        cancelFrameCallbackOnMainThread()

        try {
            runOnRenderThreadBlocking({ renderer.releaseOnRenderThread() }, allowAfterRelease = true)
        } finally {
            hostState = HiroSkiaHostState.Closed
            if (!attachedToPlatform) stopGlThread()
        }

        Log.d(TAG, "渲染器已释放")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachedToPlatform = true
    }

    override fun onDetachedFromWindow() {
        try {
            releaseRenderer()
        } finally {
            attachedToPlatform = false

            if (!glThreadStopped) {
                try {
                    super.onDetachedFromWindow()
                } finally {
                    glThreadStopped = true
                }
            }
        }
    }

    private fun stopGlThread() {
        if (glThreadStopped) return

        try {
            super.onDetachedFromWindow()
        } finally {
            glThreadStopped = true
        }
    }

    private fun scheduleFrameOnMainThread() {
        checkHiroMainThread()
        if (released.get() || hostState != HiroSkiaHostState.Running || frameScheduled) return
        frameScheduled = true
        choreographer.postFrameCallback(frameCallback)
    }

    private fun cancelFrameCallbackOnMainThread() {
        checkHiroMainThread()
        if (frameScheduled) choreographer.removeFrameCallback(frameCallback)
        frameScheduled = false
    }

    private fun doFrame(nanoTime: Long) {
        checkHiroMainThread()
        frameScheduled = false
        if (released.get() || hostState != HiroSkiaHostState.Running || !frameWanted.get()) return
        frameTimeNanos.set(nanoTime)
        requestRender()
    }

    private fun consumeFrameTime(): Long {
        frameWanted.set(false)
        return frameTimeNanos.getAndSet(0L).takeIf { it > 0L } ?: System.nanoTime()
    }

    private fun runOnRenderThreadBlocking(block: Runnable, allowAfterRelease: Boolean = false) {
        if (renderer.isOnRenderThread()) {
            block.run()
            return
        }

        if (released.get() && !allowAfterRelease) return

        val completed = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>()

        queueEvent {
            try {
                block.run()
            } catch (throwable: Throwable) {
                failure.set(throwable)
            } finally {
                completed.countDown()
            }
        }

        completed.await()
        failure.get()?.let { throw IllegalStateException("Hiro Skia 渲染线程任务执行失败", it) }
    }
}

private enum class HiroSkiaHostState { Running, Paused, Closing, Closed }

private class HiroSkiaSurfaceRenderer(private val layer: HiroSkiaLayer, private val config: HiroSkiaLayerConfig, private val consumeFrameTime: () -> Long) : GLSurfaceView.Renderer {
    private var width = 0
    private var height = 0
    private var directContext: DirectContext? = null
    private var renderTarget: BackendRenderTarget? = null
    private var surface: Surface? = null
    private var canvas: Canvas? = null
    @Volatile
    private var renderThread: Thread? = null
    private var state = HiroSkiaRendererState.WaitingForSurface

    companion object {
        private const val TAG = "HiroSkiaSurfaceRenderer"
    }

    fun isOnRenderThread(): Boolean = Thread.currentThread() === renderThread

    fun runOnRenderThread(block: Runnable) {
        bindRenderThread()
        if (state != HiroSkiaRendererState.Closed) block.run()
    }

    override fun onSurfaceCreated(gl: GL10?, eglConfig: EGLConfig?) {
        bindRenderThread()
        gl ?: return
        if (state == HiroSkiaRendererState.Closed) return
        state = HiroSkiaRendererState.WaitingForSurface
        disposeGpuResources(contextLost = directContext != null)
        directContext = DirectContext.makeGL()
        gl.glClearColor(0f, 0f, 0f, 0f)
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
        Log.d(TAG, "GPU 上下文已创建")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        bindRenderThread()
        gl ?: return
        if (state == HiroSkiaRendererState.Closed) return

        this.width = width
        this.height = height
        createSurface(gl)
        state = HiroSkiaRendererState.SurfaceReady
        layer.needRender()
        Log.d(TAG, "GPU 表面已改变：${width}x${height}")
    }

    override fun onDrawFrame(gl: GL10?) {
        bindRenderThread()
        gl ?: return
        if (state != HiroSkiaRendererState.SurfaceReady || width <= 0 || height <= 0) return

        val targetSurface = surface ?: return
        val targetCanvas = canvas ?: return
        val nanoTime = consumeFrameTime()
        val delegate = layer.renderDelegate ?: return

        targetCanvas.clear(config.clearColor)
        delegate.onRender(targetCanvas, width, height, nanoTime)
        targetSurface.flushAndSubmit(syncCpu = false)
    }

    fun releaseOnRenderThread() {
        bindRenderThread()
        if (state == HiroSkiaRendererState.Closed) return
        state = HiroSkiaRendererState.Closed

        val hasCurrentContext = EGL14.eglGetCurrentContext() != EGL14.EGL_NO_CONTEXT
        if (!hasCurrentContext) directContext?.abandon()
        try {
            (layer.renderDelegate as? HiroSkiaRenderLifecycleDelegate)?.onRenderThreadClosing()
        } finally {
            disposeGpuResources(contextLost = false)
        }
        Log.d(TAG, "渲染线程资源已关闭")
    }

    private fun bindRenderThread() {
        val current = Thread.currentThread()
        val owner = renderThread
        check(owner == null || owner === current) { "Hiro Skia 渲染器被多个线程访问" }

        if (owner == null) {
            renderThread = current
            Log.d(TAG, "绑定渲染线程：${current.name}")
        }
    }

    private fun createSurface(gl: GL10) {
        disposeSurfaceResources()
        val context = directContext ?: DirectContext.makeGL().also { directContext = it }
        val framebufferId = queryGlInteger(gl, GLES20.GL_FRAMEBUFFER_BINDING)
        val sampleCount = queryGlInteger(gl, GLES20.GL_SAMPLES).coerceAtLeast(0)
        val stencilBits = queryGlInteger(gl, GLES20.GL_STENCIL_BITS).let { if (it > 0) it else config.stencilBufferBits }

        val nextRenderTarget = BackendRenderTarget.makeGL(width, height, sampleCount, stencilBits, framebufferId, FramebufferFormat.GR_GL_RGBA8)

        val nextSurface = try {
            Surface.makeFromBackendRenderTarget(
                context,
                nextRenderTarget,
                SurfaceOrigin.BOTTOM_LEFT,
                SurfaceColorFormat.RGBA_8888,
                ColorSpace.sRGB,
            ) ?: error("无法创建 Skia GPU Surface")
        } catch (throwable: Throwable) {
            try {
                nextRenderTarget.close()
            } catch (closeFailure: Throwable) {
                throwable.addSuppressed(closeFailure)
            }
            throw throwable
        }

        renderTarget = nextRenderTarget
        surface = nextSurface
        canvas = nextSurface.canvas
    }

    private fun disposeSurfaceResources() {
        val oldSurface = surface
        val oldRenderTarget = renderTarget
        canvas = null
        surface = null
        renderTarget = null

        oldRenderTarget.use { oldSurface?.close() }
    }

    private fun disposeGpuResources(contextLost: Boolean) {
        val oldContext = directContext
        directContext = null
        width = 0
        height = 0

        try {
            if (contextLost) oldContext?.abandon()
        } finally {
            oldContext.use { disposeSurfaceResources() }
        }
    }

    private fun queryGlInteger(gl: GL10, name: Int) = IntBuffer.allocate(1).also { gl.glGetIntegerv(name, it) }[0]
}

private enum class HiroSkiaRendererState {
    WaitingForSurface,
    SurfaceReady,
    Closed,
}
