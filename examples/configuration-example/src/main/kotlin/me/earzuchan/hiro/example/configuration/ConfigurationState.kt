package me.earzuchan.hiro.example.configuration

import androidx.compose.ui.SystemTheme
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import me.earzuchan.hiro.compose.HiroComposeConfiguration
import me.earzuchan.hiro.compose.HiroViewConfigurationSnapshot
import me.earzuchan.hiro.compose.hiroComposeConfiguration
import me.earzuchan.hiro.compose.windowinsets.HiroInsetsValues
import me.earzuchan.hiro.compose.windowinsets.HiroWindowInsetsSnapshot

internal enum class GenericPolicyChoice {
    FollowSystem,
    Fixed,
    TransformSystem,
}

internal enum class InsetsPolicyChoice {
    FollowSystem,
    Fixed,
    TransformSystem,
}

internal enum class ViewConfigurationPolicyChoice {
    FollowSystem,
    FollowCompose,
    Fixed,
    TransformSystem,
}

internal data class ConfigurationState(
    val densityPolicy: GenericPolicyChoice = GenericPolicyChoice.FollowSystem,
    val densityValue: Float = 1.25f,
    val fontScalePolicy: GenericPolicyChoice = GenericPolicyChoice.FollowSystem,
    val fontScaleValue: Float = 1.15f,
    val layoutDirectionPolicy: GenericPolicyChoice = GenericPolicyChoice.FollowSystem,
    val layoutDirectionRtl: Boolean = false,
    val systemThemePolicy: GenericPolicyChoice = GenericPolicyChoice.FollowSystem,
    val systemThemeDark: Boolean = true,
    val localePolicy: GenericPolicyChoice = GenericPolicyChoice.FollowSystem,
    val localeChinese: Boolean = true,
    val insetsPolicy: InsetsPolicyChoice = InsetsPolicyChoice.FollowSystem,
    val insetTop: Int = 24,
    val insetBottom: Int = 24,
    val viewConfigurationPolicy: ViewConfigurationPolicyChoice = ViewConfigurationPolicyChoice.FollowSystem,
    val touchSlop: Float = 18f,
) {
    fun toHiroConfiguration(): HiroComposeConfiguration {
        val configuredTouchSlop = touchSlop
        return hiroComposeConfiguration {
        environment {
            density {
                when (densityPolicy) {
                    GenericPolicyChoice.FollowSystem -> followSystem()
                    GenericPolicyChoice.Fixed -> fixed(densityValue)
                    GenericPolicyChoice.TransformSystem -> transformSystem { it * densityValue }
                }
            }
            fontScale {
                when (fontScalePolicy) {
                    GenericPolicyChoice.FollowSystem -> followSystem()
                    GenericPolicyChoice.Fixed -> fixed(fontScaleValue)
                    GenericPolicyChoice.TransformSystem -> transformSystem { it * fontScaleValue }
                }
            }
            layoutDirection {
                when (layoutDirectionPolicy) {
                    GenericPolicyChoice.FollowSystem -> followSystem()
                    GenericPolicyChoice.Fixed -> fixed(layoutDirectionValue())
                    GenericPolicyChoice.TransformSystem -> transformSystem { if (it == LayoutDirection.Ltr) LayoutDirection.Rtl else LayoutDirection.Ltr }
                }
            }
            systemTheme {
                when (systemThemePolicy) {
                    GenericPolicyChoice.FollowSystem -> followSystem()
                    GenericPolicyChoice.Fixed -> fixed(systemThemeValue())
                    GenericPolicyChoice.TransformSystem -> transformSystem { if (it == SystemTheme.Dark) SystemTheme.Light else SystemTheme.Dark }
                }
            }
            localeList {
                when (localePolicy) {
                    GenericPolicyChoice.FollowSystem -> followSystem()
                    GenericPolicyChoice.Fixed -> fixed(localeValue())
                    GenericPolicyChoice.TransformSystem -> transformSystem { localeValue() }
                }
            }
            windowInsets {
                when (insetsPolicy) {
                    InsetsPolicyChoice.FollowSystem -> followSystem(insetsValue())
                    InsetsPolicyChoice.Fixed -> fixed(insetsValue())
                    InsetsPolicyChoice.TransformSystem -> transformSystem(insetsValue()) { system ->
                        system.copy(
                            statusBars = system.statusBars.copy(top = system.statusBars.top + insetTop, bottom = system.statusBars.bottom + insetBottom),
                            systemBars = system.systemBars.copy(top = system.systemBars.top + insetTop, bottom = system.systemBars.bottom + insetBottom),
                        )
                    }
                }
            }
            viewConfiguration {
                when (viewConfigurationPolicy) {
                    ViewConfigurationPolicyChoice.FollowSystem -> followSystem { touchSlop = configuredTouchSlop.dp }
                    ViewConfigurationPolicyChoice.FollowCompose -> followCompose { touchSlop = configuredTouchSlop.dp }
                    ViewConfigurationPolicyChoice.Fixed -> fixed(viewConfigurationValue())
                    ViewConfigurationPolicyChoice.TransformSystem -> transformSystem { it.copy(touchSlop = configuredTouchSlop) }
                }
            }
        }
        }
    }

    private fun layoutDirectionValue() = if (layoutDirectionRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    private fun systemThemeValue() = if (systemThemeDark) SystemTheme.Dark else SystemTheme.Light

    private fun localeValue() = LocaleList(Locale(if (localeChinese) "zh-CN" else "en-US"))

    private fun insetsValue() = HiroWindowInsetsSnapshot(
        statusBars = HiroInsetsValues(top = insetTop, bottom = insetBottom),
        navigationBars = HiroInsetsValues(bottom = insetBottom),
        systemBars = HiroInsetsValues(top = insetTop, bottom = insetBottom),
    )

    private fun viewConfigurationValue() = HiroViewConfigurationSnapshot(
        longPressTimeoutMillis = 500L,
        doubleTapTimeoutMillis = 300L,
        doubleTapMinTimeMillis = 40L,
        touchSlop = touchSlop,
        handwritingSlop = 2f,
        minimumTouchTargetSize = androidx.compose.ui.unit.DpSize(48.dp, 48.dp),
        maximumFlingVelocity = 10000f,
        minimumFlingVelocity = 0f,
        handwritingGestureLineMargin = 16f,
    )
}

internal fun GenericPolicyChoice.label() = when (this) {
    GenericPolicyChoice.FollowSystem -> "跟随系统"
    GenericPolicyChoice.Fixed -> "固定用户值"
    GenericPolicyChoice.TransformSystem -> "变换系统值"
}

internal fun InsetsPolicyChoice.label() = when (this) {
    InsetsPolicyChoice.FollowSystem -> "跟随系统（带初值）"
    InsetsPolicyChoice.Fixed -> "固定用户值"
    InsetsPolicyChoice.TransformSystem -> "变换系统值（带初值）"
}

internal fun ViewConfigurationPolicyChoice.label() = when (this) {
    ViewConfigurationPolicyChoice.FollowSystem -> "跟随 Android（补丁）"
    ViewConfigurationPolicyChoice.FollowCompose -> "跟随 Compose（补丁）"
    ViewConfigurationPolicyChoice.Fixed -> "固定完整快照"
    ViewConfigurationPolicyChoice.TransformSystem -> "变换 Android 快照"
}

internal fun Float.formatValue() = "%.2f".format(java.util.Locale.ROOT, this)
