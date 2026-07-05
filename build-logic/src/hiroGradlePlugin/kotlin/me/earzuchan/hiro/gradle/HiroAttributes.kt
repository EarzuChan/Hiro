package me.earzuchan.hiro.gradle

import org.gradle.api.attributes.Attribute

internal object HiroAttributes {
    val skikoBackend: Attribute<String> =
        Attribute.of("me.earzuchan.hiro.skikoBackend", String::class.java)

    val skikoCandidateKind: Attribute<String> =
        Attribute.of("me.earzuchan.hiro.skikoCandidateKind", String::class.java)

    const val required = "required"
}
