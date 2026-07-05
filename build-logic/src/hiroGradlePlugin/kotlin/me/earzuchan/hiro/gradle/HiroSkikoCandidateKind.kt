package me.earzuchan.hiro.gradle

internal object HiroSkikoCandidateKind {
    const val composeMetadata = "compose-metadata"
    const val skiko = "skiko"
    const val jvm = "jvm"
    const val desktop = "desktop"

    val priority = listOf(
        skiko,
        jvm,
        desktop,
        composeMetadata,
    )
}
