package me.earzuchan.hiro.gradle.diagnostic

internal data class HiroForbiddenDependency(
    val label: String,
    val binaryPatterns: List<String>,
)
