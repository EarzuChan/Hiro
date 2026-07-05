package me.earzuchan.hiro.gradle

internal object HiroDependencyPolicy {
    const val hiroGroup = "me.earzuchan.hiro"
    const val version = "1.11.1"

    data class Replacement(
        val group: String,
        val module: String,
        val version: String,
    ) {
        val notation: String get() = "$group:$module:$version"
    }

    fun replacementFor(
        group: String,
        module: String,
    ): Replacement? {
        val replacementModule = when {
            isComposeBundleModule(group, module) -> "compose"
            isMaterial3Module(group, module) -> "material3"
            else -> return null
        }
        return Replacement(hiroGroup, replacementModule, version)
    }

    fun isComposeMultiplatformModule(group: String): Boolean =
        group.startsWith("org.jetbrains.compose.")

    fun isHiroModule(group: String): Boolean =
        group == hiroGroup

    fun isThirdPartyHijackCandidate(group: String): Boolean =
        group != hiroGroup &&
            !group.startsWith("androidx.") &&
            !group.startsWith("com.android.") &&
            !group.startsWith("org.jetbrains.") &&
            !group.startsWith("org.gradle.")

    fun shouldDropComposeMetadataDependency(group: String, name: String): Boolean =
        name.contains("uikit", ignoreCase = true) ||
            name.endsWith("-js") ||
            name.endsWith("-wasm-js") ||
            group == "org.jetbrains.kotlinx" && name == "kotlinx-browser"

    fun isSelfPackageProject(projectPath: String): Boolean =
        projectPath in setOf(
            ":skiko",
            ":compose",
            ":material3",
            ":hiro",
            ":hiro-gradle-plugin",
        )

    private fun isComposeBundleModule(group: String, module: String): Boolean =
        group in setOf(
            "androidx.compose.runtime",
            "androidx.compose.ui",
            "androidx.compose.foundation",
            "androidx.compose.animation",
            "org.jetbrains.compose.runtime",
            "org.jetbrains.compose.ui",
            "org.jetbrains.compose.foundation",
            "org.jetbrains.compose.animation",
        ) &&
            !module.contains("lint", ignoreCase = true) &&
            !module.contains("test", ignoreCase = true)

    private fun isMaterial3Module(group: String, module: String): Boolean =
        (group == "androidx.compose.material3" ||
            group == "org.jetbrains.compose.material3") &&
            !module.contains("lint", ignoreCase = true) &&
            !module.contains("test", ignoreCase = true)
}
