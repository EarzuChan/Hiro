package me.earzuchan.hiro.gradleplugin

internal object HiroDependencyPolicy {
    const val hiroGroup = "me.earzuchan.hiro"

    fun isComposeLibrary(group: String): Boolean = group == "org.jetbrains.compose" || group.startsWith("org.jetbrains.compose.") || group == "androidx.compose" || group.startsWith("androidx.compose.")

    fun isHiroModule(group: String): Boolean = group == hiroGroup

    fun isThirdPartyHijackCandidate(group: String): Boolean = group.isNotBlank() && !isHiroModule(group) && !isComposeLibrary(group) && !group.startsWith("androidx.") && !group.startsWith("com.android.") && !group.startsWith("org.jetbrains.") && !group.startsWith("org.gradle.")
}
