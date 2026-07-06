package me.earzuchan.hiro.gradleplugin

import org.gradle.api.attributes.Attribute

internal object HiroAttributes {
    val skiaBackend: Attribute<String> = Attribute.of("me.earzuchan.hiro.skiaBackend", String::class.java)

    val kmpVariantKind: Attribute<String> = Attribute.of("me.earzuchan.hiro.kmpVariantKind", String::class.java)

    const val required = "required"
    const val blockedCompose = "blocked-compose"
}
