package me.earzuchan.hiro.skia

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.view.SurfaceHolder
import android.view.SurfaceView
import me.earzuchan.hiro.skia.util.checkHiroMainThread
import org.jetbrains.skia.DirectContext
import java.nio.IntBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLSurface
import javax.microedition.khronos.opengles.GL10

internal object HiroSharedEglRenderer {
    private var engine: HiroSharedEglEngine? = null

    fun attach(
        context: Context,
        surfaceView: SurfaceView,
        layer: HiroSkiaLayer,
        config: HiroSkiaLayerConfig,
        consumeFrameTime: () -> Long,
        canRender: () -> Boolean,
    ): HiroSharedEglRenderTarget {
        checkHiroMainThread()
        HiroSkiaRuntime.initialize(context)

        val existing = engine
        if (existing != null) {
            existing.requireCompatible(config)
            return existing.attach(surfaceView, layer, config, consumeFrameTime, canRender)
        }

        val created = HiroSharedEglEngine(config)
        engine = created
        return try {
            created.attach(surfaceView, layer, config, consumeFrameTime, canRender)
        } catch (throwable: Throwable) {
            if (engine === created) engine = null
            created.stopBlocking()
            throw throwable
        }
    }

    fun onTargetReleased(engine: HiroSharedEglEngine) {
        checkHiroMainThread()
        if (engine.targetCount != 0) return

        try {
            engine.stopBlocking()
        } finally {
            if (this.engine === engine) this.engine = null
        }
    }
}

internal class HiroSharedEglEngine(val configuration: HiroSkiaLayerConfig) {
    private val thread = HandlerThread("HiroSkiaRenderThread")
    private val stopping = AtomicBoolean(false)
    private val eglHelper = HiroEglHelper(configuration)
    private val targets = LinkedHashSet<HiroSharedEglRenderTarget>()
    private lateinit var handler: Handler
    private var gl: GL10? = null
    private var directContext: DirectContext? = null

    var targetCount = 0
        private set

    init {
        thread.start()
        handler = Handler(thread.looper)
    }

    fun requireCompatible(config: HiroSkiaLayerConfig) {
        require(configuration.runtimeKey() == config.runtimeKey()) { "同一进程内所有 HiroSkiaLayer 必须使用一致的 EGL 配置" }
    }

    fun attach(
        surfaceView: SurfaceView,
        layer: HiroSkiaLayer,
        config: HiroSkiaLayerConfig,
        consumeFrameTime: () -> Long,
        canRender: () -> Boolean,
    ): HiroSharedEglRenderTarget {
        checkHiroMainThread()
        check(!stopping.get()) { "Hiro 共享 EGL 执行器已经停止" }

        val target = HiroSharedEglRenderTarget(
            engine = this,
            surfaceView = surfaceView,
            layer = layer,
            config = config,
            consumeFrameTime = consumeFrameTime,
            canRender = canRender,
        )
        runBlocking { check(targets.add(target)) { "Hiro EGL 目标被重复注册" } }
        targetCount++
        target.bindToSurfaceHolder()
        return target
    }

    fun isOnRenderThread(): Boolean = Thread.currentThread() === thread

    fun post(block: Runnable): Boolean {
        if (stopping.get()) return false
        return handler.post(block)
    }

    fun runBlocking(block: Runnable) {
        if (isOnRenderThread()) {
            block.run()
            return
        }

        val completed = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>()
        check(handler.post {
            try {
                block.run()
            } catch (throwable: Throwable) {
                failure.set(throwable)
            } finally {
                completed.countDown()
            }
        }) { "无法把任务投递到 Hiro 共享 EGL 线程" }

        completed.await()
        failure.get()?.let { throw IllegalStateException("Hiro 共享 EGL 线程任务执行失败", it) }
    }

    fun releaseTarget(target: HiroSharedEglRenderTarget) {
        checkHiroMainThread()
        var failure: Throwable? = null
        var removed = false

        try {
            runBlocking {
                check(target in targets) { "Hiro EGL 目标尚未注册" }
                try {
                    target.closeOnRenderThread(closeSharedContext = targets.size == 1)
                } catch (throwable: Throwable) {
                    failure = throwable
                }

                try {
                    check(targets.remove(target)) { "Hiro EGL 目标注销失败" }
                    removed = true
                } catch (throwable: Throwable) {
                    failure?.addSuppressed(throwable) ?: run { failure = throwable }
                }

                if (targets.isEmpty()) try {
                    eglHelper.finish()
                } catch (throwable: Throwable) {
                    failure?.addSuppressed(throwable) ?: run { failure = throwable }
                }
            }
        } catch (throwable: Throwable) {
            failure?.addSuppressed(throwable) ?: run { failure = throwable }
        }

        if (removed) {
            targetCount--
            check(targetCount >= 0) { "Hiro 共享 EGL 目标计数出现负值" }
            try {
                HiroSharedEglRenderer.onTargetReleased(this)
            } catch (throwable: Throwable) {
                failure?.addSuppressed(throwable) ?: run { failure = throwable }
            }
        }

        failure?.let { throw it }
    }

    fun pauseTarget(target: HiroSharedEglRenderTarget, beforePause: Runnable) {
        runBlocking {
            if (target.isPausedOnRenderThread()) return@runBlocking
            val closeSharedContext = !configuration.preserveEglContextOnPause &&
                targets.all { it === target || it.isPausedOnRenderThread() }
            var failure: Throwable? = null

            try {
                target.pauseOnRenderThread(beforePause, closeSharedContext)
            } catch (throwable: Throwable) {
                failure = throwable
            }

            if (closeSharedContext) try {
                eglHelper.finish()
            } catch (throwable: Throwable) {
                failure?.addSuppressed(throwable) ?: run { failure = throwable }
            }

            failure?.let { throw it }
        }
    }

    fun resumeTarget(target: HiroSharedEglRenderTarget, afterResume: Runnable) {
        runBlocking { target.resumeOnRenderThread(afterResume) }
    }

    fun createWindowSurface(holder: SurfaceHolder): EGLSurface? {
        checkRenderThread()
        return eglHelper.createWindowSurface(holder)
    }

    fun destroySurface(surface: EGLSurface) {
        checkRenderThread()
        eglHelper.destroySurface(surface)
    }

    fun makeCurrent(surface: EGLSurface): Boolean {
        checkRenderThread()
        return eglHelper.makeCurrent(surface)
    }

    fun isCurrent(surface: EGLSurface): Boolean {
        checkRenderThread()
        return eglHelper.isCurrent(surface)
    }

    fun makeAnySurfaceCurrent(except: HiroSharedEglRenderTarget? = null): Boolean {
        checkRenderThread()
        targets.forEach { target ->
            if (target !== except && target.hasEglSurfaceOnRenderThread() && target.makeCurrentOnRenderThread()) return true
        }
        eglHelper.clearCurrent()
        return false
    }

    fun directContext(): DirectContext {
        checkRenderThread()
        directContext?.let { return it }

        return DirectContext.makeGL().also { directContext = it }
    }

    fun queryGlInteger(name: Int): Int {
        checkRenderThread()
        val value = IntBuffer.allocate(1)
        gl().glGetIntegerv(name, value)
        return value[0]
    }

    fun swapBuffers(surface: EGLSurface): Int {
        checkRenderThread()
        return eglHelper.swap(surface)
    }

    fun closeDirectContextOnRenderThread(hasCurrentContext: Boolean) {
        checkRenderThread()
        val oldContext = directContext
        directContext = null
        gl = null
        oldContext ?: return
        if (!hasCurrentContext) oldContext.abandon()
        oldContext.close()
    }

    fun handleContextLostOnRenderThread() {
        checkRenderThread()
        directContext?.abandon()
        targets.forEach(HiroSharedEglRenderTarget::releaseForContextLossOnRenderThread)
        directContext?.close()
        directContext = null
        gl = null
        eglHelper.finish()
        targets.filterNot(HiroSharedEglRenderTarget::isPausedOnRenderThread)
            .forEach(HiroSharedEglRenderTarget::recreateAfterContextLossOnRenderThread)
    }

    fun stopBlocking() {
        checkHiroMainThread()
        if (!stopping.compareAndSet(false, true)) return

        runBlockingIgnoringStopping {
            check(targets.isEmpty()) { "仍有 Hiro EGL 目标时不能停止共享线程" }
            closeDirectContextOnRenderThread(hasCurrentContext = false)
            eglHelper.finish()
        }
        thread.quitSafely()
        thread.join()
    }

    private fun runBlockingIgnoringStopping(block: Runnable) {
        val completed = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>()
        check(handler.post {
            try {
                block.run()
            } catch (throwable: Throwable) {
                failure.set(throwable)
            } finally {
                completed.countDown()
            }
        }) { "无法关闭 Hiro 共享 EGL 线程" }
        completed.await()
        failure.get()?.let { throw IllegalStateException("关闭 Hiro 共享 EGL 线程失败", it) }
    }

    private fun gl(): GL10 {
        gl?.let { return it }

        return eglHelper.createGl().also {
            gl = it
            it.glClearColor(0f, 0f, 0f, 0f)
            it.glClear(GL10.GL_COLOR_BUFFER_BIT)
        }
    }

    private fun checkRenderThread() = check(isOnRenderThread()) { "Hiro EGL 资源只能在共享渲染线程操作" }

    private fun HiroSkiaLayerConfig.runtimeKey() = copy(clearColor = 0, stencilBufferBits = 0)
}
