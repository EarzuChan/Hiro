package me.earzuchan.hiro.gradleplugin

internal object HiroSkikoCandidateKind {
    const val skiko = "skiko"
    const val jvm = "jvm"
    const val desktop = "desktop"

    val priority = listOf(
        skiko,
        jvm,
        desktop,
    )
}
