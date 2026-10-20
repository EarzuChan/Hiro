package me.earzuchan.hiro.compose.internal.res

import android.util.TypedValue
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.ResourceFont
import androidx.compose.ui.text.platform.Font as SkiaFont
import me.earzuchan.hiro.compose.internal.HiroAndroidApplication

internal object HiroFontResources {
    // TIPS：res/font 下的字体文件按原样存放，可直接读出字节交给 Skia；XML 字体族（含可下载字体）暂不支持
    fun toPlatformFont(font: ResourceFont): Font {
        val resources = HiroAndroidApplication.context.resources
        val value = TypedValue().also { resources.getValue(font.resId, it, true) }
        val path = value.string?.toString().orEmpty()
        require(!path.endsWith(".xml")) { "Hiro 暂不支持 XML 字体族资源（$path），请直接引用字体文件，或用 FontFamily 组合多个 Font(R.font.xxx)" }

        return SkiaFont(identity = "android-res:$path", getData = { resources.openRawResource(font.resId).use { it.readBytes() } }, weight = font.weight, style = font.style, variationSettings = font.variationSettings)
    }
}
