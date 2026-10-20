package me.earzuchan.hiro.compose.internal.util

import android.graphics.Bitmap as AndroidBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.nio.ByteBuffer
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

// TIPS：安卓 ARGB_8888 在内存中按 RGBA 字节序排列，可直接作为 Skia RGBA_8888 栅格使用；其他格式先转成 ARGB_8888
internal fun AndroidBitmap.toSkiaImage(): Image {
    val source = if (config == AndroidBitmap.Config.ARGB_8888) this else copy(AndroidBitmap.Config.ARGB_8888, false)
    val buffer = ByteBuffer.allocate(source.rowBytes * source.height)
    source.copyPixelsToBuffer(buffer)

    val alphaType = if (!source.hasAlpha()) ColorAlphaType.OPAQUE else if (source.isPremultiplied) ColorAlphaType.PREMUL else ColorAlphaType.UNPREMUL
    val image = Image.makeRaster(ImageInfo(source.width, source.height, ColorType.RGBA_8888, alphaType), buffer.array(), source.rowBytes)

    if (source !== this) source.recycle()
    return image
}

internal fun AndroidBitmap.toComposeImageBitmap(): ImageBitmap = toSkiaImage().toComposeImageBitmap()
