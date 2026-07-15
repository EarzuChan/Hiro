package me.earzuchan.hiro.example.configuration

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocaleList
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeContent
import androidx.compose.ui.unit.dp

@Composable
@SuppressLint("RestrictedApi")
internal fun ConfigurationPreview() {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val localeList = LocalLocaleList.current
    val interactionTuning = LocalViewConfiguration.current
    val systemBars = WindowInsets.safeContent
    val dark = isSystemInDarkTheme()
    val colors = if (dark) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeContentPadding().padding(16.dp), Arrangement.spacedBy(6.dp)) {
                Text("真实预览世界", style = MaterialTheme.typography.titleLarge)
                Text("此处读取的是 Hiro 场景提供的 CompositionLocal")
                Text("Density：${density.density.formatValue()}，FontScale：${density.fontScale.formatValue()}")
                Text("LayoutDirection：$layoutDirection")
                Text("SystemTheme：${if (dark) "Dark" else "Light"}")
                Text("Locale：${localeList.joinToString { it.toLanguageTag() }}")
                Text("WindowInsets.safeContent：左 ${systemBars.getLeft(density, layoutDirection)} / 上 ${systemBars.getTop(density)} / 右 ${systemBars.getRight(density, layoutDirection)} / 下 ${systemBars.getBottom(density)} px")
                Text("交互调校 touchSlop：${interactionTuning.touchSlop.formatValue()} px")
                Text("长按：${interactionTuning.longPressTimeoutMillis}ms，双击：${interactionTuning.doubleTapTimeoutMillis}ms")
                Box(
                    modifier = Modifier.size(64.dp).background(if (dark) Color(0xFFB9F6CA) else Color(0xFF00695C)),
                )
                Text("绿色方块的位置与大小会受到 Density、LayoutDirection 和 Insets 影响")
            }
        }
    }
}
