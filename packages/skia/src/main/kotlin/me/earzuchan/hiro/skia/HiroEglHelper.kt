package me.earzuchan.hiro.skia

import android.util.Log
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface
import javax.microedition.khronos.opengles.GL10

internal class HiroEglHelper(private val layerConfig: HiroSkiaLayerConfig) {
    private var egl: EGL10? = null
    private var display: EGLDisplay? = null
    private var config: EGLConfig? = null
    private var context: EGLContext? = null

    val isStarted: Boolean
        get() = context != null

    fun start() {
        if (isStarted) return

        val nextEgl = EGLContext.getEGL() as EGL10
        val nextDisplay = nextEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        check(nextDisplay != EGL10.EGL_NO_DISPLAY) { "无法获取 Hiro EGLDisplay" }

        val version = IntArray(2)
        check(nextEgl.eglInitialize(nextDisplay, version)) { "初始化 Hiro EGL 失败：${error(nextEgl)}" }
        val nextConfig = try {
            HiroEglConfigChooser(layerConfig).chooseConfig(nextEgl, nextDisplay)
        } catch (throwable: Throwable) {
            nextEgl.eglTerminate(nextDisplay)
            throw throwable
        }
        val contextAttributes = intArrayOf(
            EGL_CONTEXT_CLIENT_VERSION,
            layerConfig.eglContextClientVersion,
            EGL10.EGL_NONE,
        )
        val nextContext = nextEgl.eglCreateContext(nextDisplay, nextConfig, EGL10.EGL_NO_CONTEXT, contextAttributes)
        if (nextContext == null || nextContext == EGL10.EGL_NO_CONTEXT) {
            val eglError = error(nextEgl)
            nextEgl.eglTerminate(nextDisplay)
            error("创建 Hiro EGLContext 失败：$eglError")
        }

        egl = nextEgl
        display = nextDisplay
        config = nextConfig
        context = nextContext
    }

    fun createWindowSurface(nativeWindow: Any): EGLSurface? {
        start()
        val currentEgl = checkNotNull(egl)
        val currentDisplay = checkNotNull(display)
        val currentConfig = checkNotNull(config)
        val nextSurface = try {
            currentEgl.eglCreateWindowSurface(currentDisplay, currentConfig, nativeWindow, null)
        } catch (exception: IllegalArgumentException) {
            Log.e(TAG, "创建 Hiro EGLSurface 时窗口已经失效", exception)
            null
        }

        if (nextSurface == null || nextSurface == EGL10.EGL_NO_SURFACE) {
            Log.w(TAG, "创建 Hiro EGLSurface 失败：${error(currentEgl)}")
            return null
        }
        if (!makeCurrent(nextSurface)) {
            Log.w(TAG, "切换 Hiro EGLSurface 失败：${error(currentEgl)}")
            currentEgl.eglDestroySurface(currentDisplay, nextSurface)
            return null
        }
        return nextSurface
    }

    fun makeCurrent(surface: EGLSurface): Boolean {
        val currentEgl = checkNotNull(egl)
        return currentEgl.eglMakeCurrent(
            checkNotNull(display),
            surface,
            surface,
            checkNotNull(context),
        )
    }

    fun isCurrent(surface: EGLSurface): Boolean {
        val currentEgl = egl ?: return false
        return currentEgl.eglGetCurrentContext() == context &&
            currentEgl.eglGetCurrentSurface(EGL10.EGL_DRAW) == surface
    }

    fun clearCurrent() {
        val currentEgl = egl ?: return
        val currentDisplay = display ?: return
        currentEgl.eglMakeCurrent(
            currentDisplay,
            EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_CONTEXT,
        )
    }

    fun createGl(): GL10 = checkNotNull(context) { "Hiro EGLContext 尚未初始化" }.gl as GL10

    fun swap(surface: EGLSurface): Int {
        val currentEgl = checkNotNull(egl)
        return if (currentEgl.eglSwapBuffers(checkNotNull(display), surface)) EGL10.EGL_SUCCESS else currentEgl.eglGetError()
    }

    fun destroySurface(surface: EGLSurface) {
        if (surface == EGL10.EGL_NO_SURFACE) return
        val currentEgl = egl ?: return
        val currentDisplay = display ?: return
        clearCurrent()
        currentEgl.eglDestroySurface(currentDisplay, surface)
    }

    fun finish() {
        val currentEgl = egl ?: return
        val currentDisplay = display
        val currentContext = context
        var destroyError: String? = null
        try {
            clearCurrent()
            if (currentDisplay != null && currentContext != null && !currentEgl.eglDestroyContext(currentDisplay, currentContext)) {
                destroyError = error(currentEgl)
            }
            if (currentDisplay != null) currentEgl.eglTerminate(currentDisplay)
        } finally {
            context = null
            config = null
            display = null
            egl = null
        }
        check(destroyError == null) { "销毁 Hiro EGLContext 失败：$destroyError" }
    }

    private fun error(egl: EGL10) = "0x${egl.eglGetError().toString(16)}"

    private companion object {
        const val TAG = "HiroEglHelper"
        const val EGL_CONTEXT_CLIENT_VERSION = 0x3098
    }
}
