package me.earzuchan.hiro.compose.internal.glue

import me.earzuchan.hiro.compose.savable.HiroSavableStateConfiguration

internal interface HiroSavableStateConfigurationOwner {
    val hiroSavableStateConfiguration: HiroSavableStateConfiguration
}
