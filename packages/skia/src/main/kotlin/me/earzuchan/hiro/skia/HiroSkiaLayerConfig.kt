package me.earzuchan.hiro.skia

data class HiroSkiaLayerConfig(
    val eglContextClientVersion: Int = 3,
    val redBits: Int = 8,
    val greenBits: Int = 8,
    val blueBits: Int = 8,
    val alphaBits: Int = 0,
    val depthBits: Int = 24,
    val stencilBits: Int = 8,
    val sampleCount: Int = 0,
    val stencilBufferBits: Int = 8,
    val clearColor: Int = 0x00000000,
    val preserveEglContextOnPause: Boolean = true,
) {
    init {
        require(eglContextClientVersion >= 2) { "EGL 版本至少需要 2" }
        require(redBits >= 0 && greenBits >= 0 && blueBits >= 0 && alphaBits >= 0) { "颜色位数不能为负数" }
        require(depthBits >= 0 && stencilBits >= 0 && stencilBufferBits >= 0) { "深度和模板位数不能为负数" }
        require(sampleCount >= 0) { "采样数量不能为负数" }
    }
}
