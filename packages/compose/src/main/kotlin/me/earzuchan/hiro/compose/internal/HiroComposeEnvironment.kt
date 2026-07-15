package me.earzuchan.hiro.compose.internal

import androidx.compose.ui.SystemTheme
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import me.earzuchan.hiro.compose.interaction.HiroInteractionTuning

internal data class HiroComposeEnvironment(
    val density: Density,
    val isWindowFocused: Boolean,
    val layoutDirection: LayoutDirection,
    val systemTheme: SystemTheme,
    val localeList: LocaleList,
    val interactionTuning: HiroInteractionTuning,
)
