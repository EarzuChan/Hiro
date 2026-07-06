package me.earzuchan.hiro.gradleplugin

import me.earzuchan.hiro.gradleplugin.task.HiroStrictDependencyCheckTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute

class HiroGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("hiro", HiroExtension::class.java)

        project.pluginManager.withPlugin("com.android.application", Action { HiroAndroidPackaging.configure(project) })

        project.pluginManager.withPlugin("com.android.library", Action { HiroAndroidPackaging.configure(project) })

        project.dependencies.attributesSchema.attribute(HiroAttributes.kmpVariantKind, Action { strategy -> strategy.disambiguationRules.add(HiroKmpVariantKindDisambiguationRule::class.java) })

        project.dependencies.components.all(HiroSkiaVariantRule::class.java)

        project.configurations.configureEach(Action { configuration ->
            if (configuration.isAndroidMainDependencyBucket()) HiroComposeDependencyBlocker.rejectDirectComposeDependencies(configuration)

            if (configuration.isAndroidMainClasspath()) {
                configuration.attributes.attribute(HiroAttributes.skiaBackend, HiroAttributes.required)
                configuration.exclude(mapOf("group" to "org.jetbrains.runtime", "module" to "jbr-api"))
            }
        })

        val strictCheck = project.tasks.register(
            "hiroCheckStrictDependencies", HiroStrictDependencyCheckTask::class.java, Action { task ->
                task.group = "hiro"
                task.description = "检查 Android 依赖图没有泄漏 Android Compose 后端、AGSL 或桌面窗口系统。"
            }
        )

        project.afterEvaluate {
            strictCheck.configure(Action { task ->
                task.strict = extension.strict
                project.configurations
                    .filter { it.isAndroidMainClasspath() }
                    .forEach { configuration -> task.artifactFiles.from(configuration.incoming.artifactView(Action { viewConfiguration -> viewConfiguration.attributes.attribute(Attribute.of("artifactType", String::class.java), "jar") }).files) }
            })
        }

        project.tasks.matching { it.name == "check" }.configureEach(Action { task -> task.dependsOn(strictCheck) })
    }

    private fun Configuration.isAndroidMainClasspath(): Boolean {
        if (!isCanBeResolved) return false

        val name = name.lowercase()
        val mainClasspath = name.endsWith("compileclasspath") || name.endsWith("runtimeclasspath")
        val testClasspath = name.contains("test")

        return mainClasspath && !testClasspath
    }

    private fun Configuration.isAndroidMainDependencyBucket(): Boolean {
        if (isCanBeResolved || isCanBeConsumed) return false

        val name = name.lowercase()
        if (name.contains("test")) return false

        return name == "api" || name == "implementation" || name == "compileonly" || name == "runtimeonly" || name.endsWith("api") || name.endsWith("implementation") || name.endsWith("compileonly") || name.endsWith("runtimeonly")
    }
}
