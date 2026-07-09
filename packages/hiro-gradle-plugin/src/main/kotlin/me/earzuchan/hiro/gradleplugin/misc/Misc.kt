@file:Suppress("ConstPropertyName")

package me.earzuchan.hiro.gradleplugin.misc

import org.gradle.api.Project
import org.gradle.api.attributes.Attribute

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
        "META-INF/kotlin-project-structure-metadata.json"
    )

    @Suppress("UNCHECKED_CAST")
    fun addMetadataExcludes(project: Project) {
        val android = project.extensions.findByName("android") ?: return
        val packaging = android.get("packaging") ?: return
        val resources = packaging.get("resources") ?: return
        val excludes = resources.get("excludes") as? MutableSet<String> ?: return

        excludes += excludedMetadataResources
    }

    private fun Any.get(name: String): Any? {
        val getter = "get${name.replaceFirstChar { it.uppercaseChar() }}"
        return javaClass.methods.firstOrNull { method -> method.name == getter && method.parameterCount == 0 }?.invoke(this)
    }
}

internal object HiroAttributes {
    val hiroVariant: Attribute<Boolean> = Attribute.of("me.earzuchan.hiro.variant", Boolean::class.javaObjectType)

    val hiroVariantKind: Attribute<String> = Attribute.of("me.earzuchan.hiro.kmpvariantkind", String::class.java)
}

internal object HiroDependencyPolicy {
    const val HIRO_GROUP = "me.earzuchan.hiro"
    private const val JETBRAINS_GROUP = "org.jetbrains"
    private const val ANDROIDX_GROUP = "androidx"

    fun isComposeModuleOrJbrApi(group: String, name: String) = isComposeModule(group) || isJbrApi(group, name)

    fun isHiroModule(group: String) = group == HIRO_GROUP

    fun isJbrApi(group: String, name: String) = group == "$JETBRAINS_GROUP.runtime" && name == "jbr-api"

    fun isComposeModule(group: String) = group == "$ANDROIDX_GROUP.compose" || group.startsWith("$ANDROIDX_GROUP.compose.") || group == "$JETBRAINS_GROUP.compose" || group.startsWith("$JETBRAINS_GROUP.compose.")

    fun isJetBrainsModule(group: String) = group == JETBRAINS_GROUP || group.startsWith("$JETBRAINS_GROUP.")

    fun isThirdPartyKmpCandidate(group: String) = group.isNotBlank() && !isHiroModule(group) && !isComposeModule(group) && !isJetBrainsModule(group) && !group.startsWith("androidx.") && !group.startsWith("com.android.") && !group.startsWith("org.gradle.")
}