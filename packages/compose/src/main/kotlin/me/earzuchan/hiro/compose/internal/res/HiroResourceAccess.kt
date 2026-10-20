package me.earzuchan.hiro.compose.internal.res

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources

// TIPS：先读 LocalConfiguration 以订阅配置变化，安卓的 Resources 对象在配置变化时原地更新，本身不会触发重组
@Composable
@ReadOnlyComposable
internal fun currentResources(): Resources {
    LocalConfiguration.current
    return LocalResources.current
}
