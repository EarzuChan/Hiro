package me.earzuchan.hiro.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

open class HiroExtension {
    /** 在 Android 上为兼容的第三方 KMP 库优先选择 Compose/Skiko 变体。 */
    var skikoVariantHijack: Boolean = true

    /** 若 AndroidX Compose Android 后端或 Legacy Material 泄漏进依赖图，则快速失败。 */
    var strict: Boolean = true
}

class HiroGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("hiro", HiroExtension::class.java)
    }
}
