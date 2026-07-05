package me.earzuchan.hiro.skiko

import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import kotlin.math.abs

internal class HiroEglConfigChooser(
    private val layerConfig: HiroSkikoLayerConfig,
) : GLSurfaceView.EGLConfigChooser {
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        val configs = findConfigs(egl, display)
        return configs.minWithOrNull(compareBy { scoreConfig(egl, display, it) })
            ?: error("无法找到可用的 EGL 配置")
    }

    private fun findConfigs(egl: EGL10, display: EGLDisplay): List<EGLConfig> {
        val attributes = buildAttributes()
        val count = IntArray(1)
        check(egl.eglChooseConfig(display, attributes, null, 0, count)) {
            "查询 EGL 配置数量失败"
        }
        check(count[0] > 0) {
            "找不到满足 Hiro 配置的 EGL surface，当前配置为 $layerConfig"
        }

        val configs = arrayOfNulls<EGLConfig>(count[0])
        check(egl.eglChooseConfig(display, attributes, configs, configs.size, count)) {
            "查询 EGL 配置列表失败"
        }
        return configs.filterNotNull()
    }

    private fun buildAttributes(): IntArray {
        val attributes = mutableListOf(
            EGL10.EGL_RED_SIZE,
            layerConfig.redBits,
            EGL10.EGL_GREEN_SIZE,
            layerConfig.greenBits,
            EGL10.EGL_BLUE_SIZE,
            layerConfig.blueBits,
            EGL10.EGL_ALPHA_SIZE,
            layerConfig.alphaBits,
            EGL10.EGL_DEPTH_SIZE,
            layerConfig.depthBits,
            EGL10.EGL_STENCIL_SIZE,
            layerConfig.stencilBits,
            EGL10.EGL_RENDERABLE_TYPE,
            EGL_OPENGL_ES2_BIT,
        )

        if (layerConfig.sampleCount > 0) {
            attributes += EGL10.EGL_SAMPLE_BUFFERS
            attributes += 1
            attributes += EGL10.EGL_SAMPLES
            attributes += layerConfig.sampleCount
        } else {
            attributes += EGL10.EGL_SAMPLE_BUFFERS
            attributes += 0
        }

        attributes += EGL10.EGL_NONE
        return attributes.toIntArray()
    }

    private fun scoreConfig(egl: EGL10, display: EGLDisplay, eglConfig: EGLConfig): Int =
        distance(egl, display, eglConfig, EGL10.EGL_RED_SIZE, layerConfig.redBits) +
            distance(egl, display, eglConfig, EGL10.EGL_GREEN_SIZE, layerConfig.greenBits) +
            distance(egl, display, eglConfig, EGL10.EGL_BLUE_SIZE, layerConfig.blueBits) +
            distance(egl, display, eglConfig, EGL10.EGL_ALPHA_SIZE, layerConfig.alphaBits) +
            distance(egl, display, eglConfig, EGL10.EGL_DEPTH_SIZE, layerConfig.depthBits) +
            distance(egl, display, eglConfig, EGL10.EGL_STENCIL_SIZE, layerConfig.stencilBits) +
            distance(egl, display, eglConfig, EGL10.EGL_SAMPLES, layerConfig.sampleCount)

    private fun distance(
        egl: EGL10,
        display: EGLDisplay,
        eglConfig: EGLConfig,
        attribute: Int,
        expected: Int,
    ): Int =
        abs(readAttribute(egl, display, eglConfig, attribute) - expected)

    private fun readAttribute(
        egl: EGL10,
        display: EGLDisplay,
        eglConfig: EGLConfig,
        attribute: Int,
    ): Int {
        val value = IntArray(1)
        egl.eglGetConfigAttrib(display, eglConfig, attribute, value)
        return value[0]
    }

    private companion object {
        const val EGL_OPENGL_ES2_BIT = 4
    }
}
