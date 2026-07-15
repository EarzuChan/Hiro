package me.earzuchan.hiro.skia

import android.opengl.GLES20
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import me.earzuchan.hiro.skia.util.checkHiroMainThread
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGL11
import javax.microedition.khronos.egl.EGLSurface

internal class HiroSharedEglRenderTarget(
    private val engine: HiroSharedEglEngine,
    private val surfaceView: SurfaceView,
    private val layer: HiroSkiaLayer,
    private val config: HiroSkiaLayerConfig,
    private val consumeFrameTime: () -> Long,
    private val canRender: () -> Boolean,
) : SurfaceHolder.Callback2 {
    private val releaseStarted = AtomicBoolean(false)
    private val released = AtomicBoolean(false)
    private val workScheduled = AtomicBoolean(false)
    private val frameRequested = AtomicBoolean(false)
    private val pendingTasks = ConcurrentLinkedQueue<Runnable>()

    @Volatile
    private var desiredWidth = 0

    @Volatile
    private var desiredHeight = 0

    private var width = 0
    private var height = 0
    private var eglSurface: EGLSurface = EGL10.EGL_NO_SURFACE
    private var backendRenderTarget: BackendRenderTarget? = null
    private var skiaSurface: Surface? = null
    private var renderPaused = false
    private var surfaceBad = false
    private var closed = false

    fun bindToSurfaceHolder() {
        checkHiroMainThread()
        surfaceView.holder.addCallback(this)
    }

    fun post(block: Runnable): Boolean {
        if (released.get()) return false
        if (engine.isOnRenderThread() && isCurrentOnRenderThread()) {
            block.run()
            return true
        }

        return queue(block)
    }

    fun queue(block: Runnable): Boolean {
        if (released.get()) return false
        pendingTasks.add(block)
        scheduleWork()
        return true
    }

    fun isOnRenderThread(): Boolean = engine.isOnRenderThread()

    fun requestRender() {
        if (releaseStarted.get()) return
        frameRequested.set(true)
        scheduleWork()
    }

    fun runBlocking(block: Runnable) {
        if (releaseStarted.get()) return
        engine.runBlocking {
            makeTargetCurrentOrAny()
            block.run()
        }
    }

    fun pauseBlocking(beforePause: Runnable) {
        if (releaseStarted.get()) return
        engine.pauseTarget(this, beforePause)
    }

    fun resumeBlocking(afterResume: Runnable) {
        if (releaseStarted.get()) return
        engine.resumeTarget(this, afterResume)
    }

    fun releaseBlocking() {
        checkHiroMainThread()
        if (!releaseStarted.compareAndSet(false, true)) return

        surfaceView.holder.removeCallback(this)
        pendingTasks.clear()
        try {
            engine.releaseTarget(this)
        } finally {
            released.set(true)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) = Unit

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        checkHiroMainThread()
        if (releaseStarted.get()) return
        desiredWidth = width
        desiredHeight = height
        engine.post { replaceSurfaceOnRenderThread(holder, width, height) }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        checkHiroMainThread()
        desiredWidth = 0
        desiredHeight = 0
        if (releaseStarted.get()) return
        engine.runBlocking { destroySurfaceOnRenderThread(preserveDimensions = false) }
    }

    override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
        requestRender()
        engine.runBlocking {}
    }

    override fun surfaceRedrawNeededAsync(holder: SurfaceHolder, drawingFinished: Runnable) {
        requestRender()
        if (!engine.post(drawingFinished)) drawingFinished.run()
    }

    fun closeOnRenderThread(closeSharedContext: Boolean) {
        checkRenderThread()
        if (closed) return
        closed = true
        renderPaused = true

        val hasCurrentContext = makeTargetCurrentOrAny()
        var failure: Throwable? = null
        try {
            (layer.renderDelegate as? HiroSkiaRenderLifecycleDelegate)?.onRenderThreadClosing()
        } catch (throwable: Throwable) {
            failure = throwable
        }

        pendingTasks.clear()
        try {
            disposeSkiaSurface()
        } catch (throwable: Throwable) {
            failure?.addSuppressed(throwable) ?: run { failure = throwable }
        }

        if (closeSharedContext) try {
            engine.closeDirectContextOnRenderThread(hasCurrentContext)
        } catch (throwable: Throwable) {
            failure?.addSuppressed(throwable) ?: run { failure = throwable }
        }

        try {
            destroySurfaceOnRenderThread(preserveDimensions = false)
        } catch (throwable: Throwable) {
            failure?.addSuppressed(throwable) ?: run { failure = throwable }
        }

        failure?.let { throw it }
    }

    fun pauseOnRenderThread(beforePause: Runnable, closeSharedContext: Boolean) {
        checkRenderThread()
        if (closed || renderPaused) return

        val hasCurrentContext = makeTargetCurrentOrAny()
        var failure: Throwable? = null
        try {
            beforePause.run()
        } catch (throwable: Throwable) {
            failure = throwable
        }

        renderPaused = true
        try {
            disposeSkiaSurface()
        } catch (throwable: Throwable) {
            failure?.addSuppressed(throwable) ?: run { failure = throwable }
        }

        if (closeSharedContext) try {
            engine.closeDirectContextOnRenderThread(hasCurrentContext)
        } catch (throwable: Throwable) {
            failure?.addSuppressed(throwable) ?: run { failure = throwable }
        }

        try {
            destroySurfaceOnRenderThread(preserveDimensions = true)
        } catch (throwable: Throwable) {
            failure?.addSuppressed(throwable) ?: run { failure = throwable }
        }

        failure?.let { throw it }
    }

    fun resumeOnRenderThread(afterResume: Runnable) {
        checkRenderThread()
        if (closed) return

        renderPaused = false
        recreateSurfaceFromHolderOnRenderThread()
        makeTargetCurrentOrAny()
        afterResume.run()
        if (pendingTasks.isNotEmpty() || frameRequested.get()) scheduleWork()
    }

    fun isPausedOnRenderThread(): Boolean {
        checkRenderThread()
        return renderPaused
    }

    fun hasEglSurfaceOnRenderThread(): Boolean {
        checkRenderThread()
        return eglSurface != EGL10.EGL_NO_SURFACE
    }

    fun makeCurrentOnRenderThread(): Boolean {
        checkRenderThread()
        val currentSurface = eglSurface
        return currentSurface != EGL10.EGL_NO_SURFACE && engine.makeCurrent(currentSurface)
    }

    private fun isCurrentOnRenderThread(): Boolean {
        checkRenderThread()
        val currentSurface = eglSurface
        return currentSurface != EGL10.EGL_NO_SURFACE && engine.isCurrent(currentSurface)
    }

    fun releaseForContextLossOnRenderThread() {
        checkRenderThread()
        disposeSkiaSurface()
        destroySurfaceOnRenderThread(preserveDimensions = true)
        surfaceBad = false
    }

    fun recreateAfterContextLossOnRenderThread() {
        checkRenderThread()
        if (!closed && !renderPaused) recreateSurfaceFromHolderOnRenderThread()
    }

    private fun replaceSurfaceOnRenderThread(holder: SurfaceHolder, width: Int, height: Int) {
        checkRenderThread()
        if (closed || renderPaused) return
        if (width != desiredWidth || height != desiredHeight) return

        destroySurfaceOnRenderThread(preserveDimensions = true)
        if (!holder.surface.isValid || width <= 0 || height <= 0) return

        val nextEglSurface = engine.createWindowSurface(holder)
        if (nextEglSurface == null) {
            surfaceBad = true
            Log.w(TAG, "创建 Hiro 窗口 EGLSurface 失败")
            return
        }

        this.width = width
        this.height = height
        eglSurface = nextEglSurface
        surfaceBad = false
        createSkiaSurface()
        layer.needRender()
        scheduleWork()
        Log.d(TAG, "GPU 表面已改变：${width}x$height")
    }

    private fun recreateSurfaceFromHolderOnRenderThread() {
        checkRenderThread()
        val holder = surfaceView.holder
        val width = desiredWidth
        val height = desiredHeight
        if (eglSurface == EGL10.EGL_NO_SURFACE && holder.surface.isValid && width > 0 && height > 0) {
            replaceSurfaceOnRenderThread(holder, width, height)
        }
    }

    private fun destroySurfaceOnRenderThread(preserveDimensions: Boolean) {
        checkRenderThread()
        val oldEglSurface = eglSurface
        if (oldEglSurface != EGL10.EGL_NO_SURFACE) {
            engine.makeCurrent(oldEglSurface)
            disposeSkiaSurface()
            eglSurface = EGL10.EGL_NO_SURFACE
            engine.destroySurface(oldEglSurface)
        }
        width = 0
        height = 0
        if (!preserveDimensions) {
            desiredWidth = 0
            desiredHeight = 0
        }
    }

    private fun createSkiaSurface() {
        checkRenderThread()
        val framebufferId = engine.queryGlInteger(GLES20.GL_FRAMEBUFFER_BINDING)
        val sampleCount = engine.queryGlInteger(GLES20.GL_SAMPLES).coerceAtLeast(0)
        val stencilBits = engine.queryGlInteger(GLES20.GL_STENCIL_BITS).let { if (it > 0) it else config.stencilBufferBits }
        val nextBackendTarget = BackendRenderTarget.makeGL(
            width,
            height,
            sampleCount,
            stencilBits,
            framebufferId,
            FramebufferFormat.GR_GL_RGBA8,
        )

        val nextSurface = try {
            val directContext = engine.directContext()
            Surface.makeFromBackendRenderTarget(
                directContext,
                nextBackendTarget,
                SurfaceOrigin.BOTTOM_LEFT,
                SurfaceColorFormat.RGBA_8888,
                ColorSpace.sRGB,
            ) ?: error("无法创建 Hiro Skia GPU Surface")
        } catch (throwable: Throwable) {
            try {
                nextBackendTarget.close()
            } catch (closeFailure: Throwable) {
                throwable.addSuppressed(closeFailure)
            }
            throw throwable
        }

        backendRenderTarget = nextBackendTarget
        skiaSurface = nextSurface
    }

    private fun disposeSkiaSurface() {
        val oldSurface = skiaSurface
        val oldBackendTarget = backendRenderTarget
        skiaSurface = null
        backendRenderTarget = null
        oldBackendTarget.use { oldSurface?.close() }
    }

    private fun scheduleWork() {
        if (releaseStarted.get() || !workScheduled.compareAndSet(false, true)) return
        if (!engine.post(Runnable(::runScheduledWork))) workScheduled.set(false)
    }

    private fun runScheduledWork() {
        checkRenderThread()
        try {
            if (closed) return
            makeTargetCurrentOrAny()
            while (true) pendingTasks.poll()?.run() ?: break
            if (!isReadyToRender()) return
            if (!makeCurrentOnRenderThread()) {
                surfaceBad = true
                return
            }
            if (closed || renderPaused || !canRender() || !frameRequested.compareAndSet(true, false)) return

            val targetSurface = skiaSurface ?: return
            targetSurface.canvas.clear(config.clearColor)
            layer.renderDelegate?.onRender(targetSurface.canvas, width, height, consumeFrameTime())
            targetSurface.flushAndSubmit(syncCpu = false)
            when (val swapError = engine.swapBuffers(eglSurface)) {
                EGL10.EGL_SUCCESS -> Unit
                EGL11.EGL_CONTEXT_LOST -> engine.handleContextLostOnRenderThread()
                else -> {
                    Log.w(TAG, "提交 Hiro EGLSurface 失败：0x${swapError.toString(16)}")
                    surfaceBad = true
                    destroySurfaceOnRenderThread(preserveDimensions = true)
                }
            }
        } finally {
            workScheduled.set(false)
            if (!closed &&
                (pendingTasks.isNotEmpty() || isReadyToRender() && frameRequested.get() && canRender())
            ) {
                scheduleWork()
            }
        }
    }

    private fun isReadyToRender() = !renderPaused && !surfaceBad && eglSurface != EGL10.EGL_NO_SURFACE

    private fun makeTargetCurrentOrAny(): Boolean =
        if (makeCurrentOnRenderThread()) true else engine.makeAnySurfaceCurrent(except = this)

    private fun checkRenderThread() = check(engine.isOnRenderThread()) { "Hiro EGL 目标只能在共享渲染线程操作" }

    private companion object {
        const val TAG = "HiroSharedEglTarget"
    }
}
