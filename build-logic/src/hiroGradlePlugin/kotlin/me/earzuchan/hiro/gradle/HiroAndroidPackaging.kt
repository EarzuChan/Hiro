package me.earzuchan.hiro.gradle

import org.gradle.api.Project

internal object HiroAndroidPackaging {
    private val excludedMetadataResources = setOf(
        "*Main/**",
        "commonMain/**",
        "skikoMain/**",
        "nonJvmMain/**",
        "nonAndroidMain/**",
        "jvmMain/**",
        "desktopMain/**",
        "nativeMain/**",
        "jsMain/**",
        "wasmJsMain/**",
        "webMain/**",
        "**/*.knm",
        "META-INF/kotlin-project-structure-metadata.json",
    )

    fun configure(project: Project) {
        val androidExtension = project.extensions.findByName("android") ?: return
        val packaging = androidExtension.invokeGetter("packaging") ?: return
        val resources = packaging.invokeGetter("resources") ?: return
        val excludes = resources.invokeGetter("excludes") ?: return

        @Suppress("UNCHECKED_CAST")
        (excludes as? MutableSet<String>)?.addAll(excludedMetadataResources)
    }

    private fun Any.invokeGetter(propertyName: String): Any? {
        val getterName = "get${propertyName.replaceFirstChar { it.uppercaseChar() }}"
        return javaClass.methods
            .firstOrNull { method -> method.name == getterName && method.parameterCount == 0 }
            ?.invoke(this)
    }
}
