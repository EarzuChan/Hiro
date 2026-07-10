package me.earzuchan.hiro.gradleplugin.misc

internal enum class HiroOwnedModuleKind(val description: String) { Compose("官方 Compose 模块"), ArchitectureComposeAdapter("官方 Lifecycle / SavedState Compose 适配模块"), JetBrainsRuntimeApi("JetBrains Runtime API") }

internal object HiroDependencyPolicy {
    private const val HIRO_GROUP = "me.earzuchan.hiro"

    private const val ANDROIDX_GROUP = "androidx"
    private const val JETBRAINS_GROUP = "org.jetbrains"

    private val architectureComposeModuleFamilies = mapOf(
        "$ANDROIDX_GROUP.lifecycle" to setOf("lifecycle-runtime-compose", "lifecycle-viewmodel-compose"),
        "$ANDROIDX_GROUP.savedstate" to setOf("savedstate-compose")
    )

    fun isComposeModule(group: String) = group == "$ANDROIDX_GROUP.compose" || group.startsWith("$ANDROIDX_GROUP.compose.") || group == "$JETBRAINS_GROUP.compose" || group.startsWith("$JETBRAINS_GROUP.compose.")

    private fun isArchitectureComposeAdapter(group: String, name: String) = architectureComposeModuleFamilies[group]?.any { family -> name == family || name.startsWith("$family-") } ?: false

    private fun isJbrApi(group: String, name: String) = group == "$JETBRAINS_GROUP.runtime" && name == "jbr-api"

    fun hiroOwnedModuleKindOrNull(group: String, name: String): HiroOwnedModuleKind? = when {
        isComposeModule(group) -> HiroOwnedModuleKind.Compose

        isArchitectureComposeAdapter(group, name) -> HiroOwnedModuleKind.ArchitectureComposeAdapter

        isJbrApi(group, name) -> HiroOwnedModuleKind.JetBrainsRuntimeApi

        else -> null
    }

    fun isHiroModule(group: String) = group == HIRO_GROUP

    private fun isJetBrainsModule(group: String) = group == JETBRAINS_GROUP || group.startsWith("$JETBRAINS_GROUP.")

    private fun isAndroidxModule(group: String) = group == ANDROIDX_GROUP || group.startsWith("$ANDROIDX_GROUP.")

    fun isThirdPartyKmpCandidate(group: String) = group.isNotBlank() && !isHiroModule(group) && !isJetBrainsModule(group) && !isAndroidxModule(group) && !group.startsWith("com.android.") && !group.startsWith("org.gradle.")
}
