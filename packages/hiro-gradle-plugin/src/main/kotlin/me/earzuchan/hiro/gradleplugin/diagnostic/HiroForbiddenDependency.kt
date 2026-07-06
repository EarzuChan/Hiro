package me.earzuchan.hiro.gradleplugin.diagnostic

internal data class HiroForbiddenDependency(val label: String, val binaryPatterns: List<String>, val allowedClassPaths: Set<String> = emptySet(), val allowedReferencePrefixes: List<String> = emptyList())
