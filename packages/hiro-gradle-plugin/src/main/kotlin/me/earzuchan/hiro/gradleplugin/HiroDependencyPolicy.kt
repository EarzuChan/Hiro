package me.earzuchan.hiro.gradleplugin

internal object HiroDependencyPolicy {
    const val HIRO_GROUP = "me.earzuchan.hiro"
    const val JB_GROUP = "org.jetbrains"

    fun isComposeLibrary(group: String) = group == "androidx.compose" || group.startsWith("androidx.compose.") || group == "org.jetbrains.compose" || group.startsWith("org.jetbrains.compose.")

    fun isHiroModule(group: String) = group == HIRO_GROUP

    fun isJbModule(group: String) = group.startsWith("$JB_GROUP.") || group == JB_GROUP

    fun isThirdPartyHijackCandidate(group: String) = group.isNotBlank() && !isHiroModule(group) && !isJbModule(group) && !isComposeLibrary(group) && !group.startsWith("androidx.") && !group.startsWith("com.android.") && !group.startsWith("org.gradle.")
}
