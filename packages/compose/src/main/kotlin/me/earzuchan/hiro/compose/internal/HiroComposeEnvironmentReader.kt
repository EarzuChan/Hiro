package me.earzuchan.hiro.compose.internal

import android.content.res.Configuration as AndroidConfiguration
import android.view.View
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import me.earzuchan.hiro.compose.HiroComposeConfiguration

internal class HiroComposeEnvironmentReader(private val view: View, private val hiroConfiguration: HiroComposeConfiguration) {
    fun read(): HiroComposeEnvironment {
        val resources = view.resources
        val systemDensity = resources.displayMetrics.density
        val systemFontScale = resources.configuration.fontScale
        val density = Density(hiroConfiguration.environment.densityPolicy.resolve(systemDensity), hiroConfiguration.environment.fontScalePolicy.resolve(systemFontScale))

        return HiroComposeEnvironment(
            density = density,
            isWindowFocused = view.hasWindowFocus(),
            layoutDirection = hiroConfiguration.environment.layoutDirectionPolicy.resolve(if (view.layoutDirection == View.LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr),
            systemTheme = hiroConfiguration.environment.systemThemePolicy.resolve(resources.configuration.systemTheme()),
            localeList = hiroConfiguration.environment.localeListPolicy.resolve(resources.configuration.localeList()),
            interactionTuning = hiroConfiguration.environment.interactionPolicy.resolve(view.context, density),
        )
    }
}

private fun AndroidConfiguration.systemTheme() = when (uiMode and AndroidConfiguration.UI_MODE_NIGHT_MASK) {
    AndroidConfiguration.UI_MODE_NIGHT_YES -> SystemTheme.Dark
    AndroidConfiguration.UI_MODE_NIGHT_NO -> SystemTheme.Light
    else -> SystemTheme.Unknown
}

private fun AndroidConfiguration.localeList(): LocaleList {
    val current = locales
    if (current.isEmpty) return LocaleList.current

    return LocaleList(List(current.size()) { index -> Locale(current[index]) })
}
