@file:JvmName("ResourceEnvironment_desktopKt")
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.compose.resources

import android.content.res.Configuration
import android.util.DisplayMetrics
import java.util.Locale
import me.earzuchan.hiro.compose.internal.HiroAndroidApplication

// TIPS：组合之外读取资源（如 getString）时所用的环境，取自安卓 Configuration；尚无 Context 时按系统默认语言、浅色与 mdpi 处理
@OptIn(InternalResourceApi::class)
internal fun getSystemEnvironment(): ResourceEnvironment {
    val resources = HiroAndroidApplication.contextOrNull?.resources
    val configuration = resources?.configuration
    val locale = configuration?.locales?.takeIf { !it.isEmpty }?.get(0) ?: Locale.getDefault()
    val isDarkTheme = configuration != null && configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val dpi = resources?.displayMetrics?.densityDpi ?: DisplayMetrics.DENSITY_DEFAULT

    return ResourceEnvironment(
        language = LanguageQualifier(locale.language),
        script = ScriptQualifier(locale.script),
        region = RegionQualifier(locale.country),
        theme = ThemeQualifier.selectByValue(isDarkTheme),
        density = DensityQualifier.selectByValue(dpi),
    )
}
