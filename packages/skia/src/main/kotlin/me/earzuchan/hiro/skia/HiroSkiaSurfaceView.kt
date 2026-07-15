package me.earzuchan.hiro.skia

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceView
import android.view.ViewGroup
import me.earzuchan.hiro.skia.util.checkHiroMainThread
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@SuppressLint("ViewConstructor")
class HiroSkiaSurfaceView internal constructor(
    context: Context,
    layer: HiroSkiaLayer,
    config: HiroSkiaLayerConfig,
) : SurfaceView(context) {
    private val frameWanted = AtomicBoolean(false)
    private val frameTimeNanos = AtomicLong(0L)
    private val released = AtomicBoolean(false)
    private val choreographer: Choreographer by lazy(LazyThreadSafetyMode.NONE) { Choreographer.getInstance() }
    private val frameCallback = Choreographer.FrameCallback(::doFrame)
    private val renderTarget = HiroSharedEglRenderer.attach(
        context = context,
        surfaceView = this,
        layer = layer,
        config = config,
        consumeFrameTime = ::consumeFrameTime,
        canRender = ::canRender,
    )

    private var frameScheduled = false
    @Volatile
    private var hostState = HiroSkiaHostState.Running

    companion object {
        private const val TAG = "HiroSkiaSurfaceView"
    }

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        isFocusable = true
        isFocusableInTouchMode = true
        Log.d(TAG, "被创建")
    }

    fun postToRenderThread(block: Runnable): Boolean = renderTarget.post(block)

    fun queueToRenderThread(block: Runnable): Boolean = renderTarget.queue(block)

    fun isOnRenderThread(): Boolean = renderTarget.isOnRenderThread()

    fun scheduleFrame() {
        if (released.get() || !frameWanted.compareAndSet(false, true)) return
        if (Looper.myLooper() == Looper.getMainLooper()) scheduleFrameOnMainThread() else post(::scheduleFrameOnMainThread)
    }

    internal fun pauseRenderThread(beforePause: Runnable) {
        checkHiroMainThread()
        if (hostState == HiroSkiaHostState.Closing || hostState == HiroSkiaHostState.Closed) return
        if (hostState == HiroSkiaHostState.Paused) return

        cancelFrameCallbackOnMainThread()
        hostState = HiroSkiaHostState.Paused
        renderTarget.pauseBlocking(beforePause)
        Log.d(TAG, "渲染目标已暂停")
    }

    internal fun resumeRenderThread(afterResume: Runnable) {
        checkHiroMainThread()
        if (hostState == HiroSkiaHostState.Closing || hostState == HiroSkiaHostState.Closed) return

        hostState = HiroSkiaHostState.Running
        renderTarget.resumeBlocking(afterResume)
        if (frameWanted.get()) scheduleFrameOnMainThread() else scheduleFrame()
        Log.d(TAG, "渲染目标已恢复")
    }

    internal fun releaseRenderer() {
        checkHiroMainThread()
        if (!released.compareAndSet(false, true)) return

        hostState = HiroSkiaHostState.Closing
        cancelFrameCallbackOnMainThread()
        try {
            renderTarget.releaseBlocking()
        } finally {
            hostState = HiroSkiaHostState.Closed
        }
        Log.d(TAG, "渲染目标已释放")
    }

    override fun onDetachedFromWindow() {
        try {
            releaseRenderer()
        } finally {
            super.onDetachedFromWindow()
        }
    }

    private fun scheduleFrameOnMainThread() {
        checkHiroMainThread()
        if (!canRender() || frameScheduled) return
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
        if (!canRender() || !frameWanted.get()) return
        frameTimeNanos.set(nanoTime)
        renderTarget.requestRender()
    }

    private fun consumeFrameTime(): Long {
        frameWanted.set(false)
        return frameTimeNanos.getAndSet(0L).takeIf { it > 0L } ?: System.nanoTime()
    }

    private fun canRender() = !released.get() && hostState == HiroSkiaHostState.Running
}

private enum class HiroSkiaHostState { Running, Paused, Closing, Closed }
