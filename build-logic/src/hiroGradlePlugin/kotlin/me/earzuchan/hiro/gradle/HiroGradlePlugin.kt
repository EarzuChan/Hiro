package me.earzuchan.hiro.gradle

import me.earzuchan.hiro.gradle.task.HiroStrictDependencyCheckTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeMatchingStrategy
import org.gradle.api.plugins.AppliedPlugin

class HiroGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("hiro", HiroExtension::class.java)
        val selfPackageProject = HiroDependencyPolicy.isSelfPackageProject(project.path)

        project.pluginManager.withPlugin(
            "com.android.application",
            object : Action<AppliedPlugin> {
                override fun execute(plugin: AppliedPlugin) {
                    HiroAndroidPackaging.configure(project)
                }
            },
        )
        project.pluginManager.withPlugin(
            "com.android.library",
            object : Action<AppliedPlugin> {
                override fun execute(plugin: AppliedPlugin) {
                    HiroAndroidPackaging.configure(project)
                }
            },
        )

        project.dependencies.attributesSchema.attribute(
            HiroAttributes.skikoCandidateKind,
            object : Action<AttributeMatchingStrategy<String>> {
                override fun execute(strategy: AttributeMatchingStrategy<String>) {
                    strategy.disambiguationRules.add(HiroSkikoCandidateKindDisambiguationRule::class.java)
                }
            },
        )
        project.dependencies.components.all(HiroSkikoVariantRule::class.java)

        project.configurations.configureEach(object : Action<Configuration> {
            override fun execute(configuration: Configuration) {
                if (configuration.isAndroidMainDependencyBucket()) {
                    HiroDependencyRewriter.rewriteDirectDependencies(
                        project = project,
                        configuration = configuration,
                        selfPackageProject = selfPackageProject,
                    )
                }

                if (configuration.isAndroidMainClasspath()) {
                    configuration.attributes.attribute(HiroAttributes.skikoBackend, HiroAttributes.required)
                    configuration.exclude(
                        mapOf(
                            "group" to "org.jetbrains.runtime",
                            "module" to "jbr-api",
                        ),
                    )
                }
            }
        })

        val strictCheck = project.tasks.register(
            "hiroCheckStrictDependencies",
            HiroStrictDependencyCheckTask::class.java,
            object : Action<HiroStrictDependencyCheckTask> {
                override fun execute(task: HiroStrictDependencyCheckTask) {
                    task.group = "hiro"
                    task.description = "检查 Android 依赖图没有泄漏 Android Compose 后端、AGSL 或桌面窗口系统。"
                }
            },
        )

        project.afterEvaluate {
            strictCheck.configure(object : Action<HiroStrictDependencyCheckTask> {
                override fun execute(task: HiroStrictDependencyCheckTask) {
                    task.strict = extension.strict
                    project.configurations
                        .filter { it.isAndroidMainClasspath() }
                        .forEach { configuration ->
                            task.artifactFiles.from(configuration.incoming.artifactView(
                                object : Action<ArtifactView.ViewConfiguration> {
                                    override fun execute(viewConfiguration: ArtifactView.ViewConfiguration) {
                                        viewConfiguration.attributes.attribute(
                                            Attribute.of("artifactType", String::class.java),
                                            "jar",
                                        )
                                    }
                                },
                            ).files)
                        }
                }
            })
        }

        project.tasks.matching { it.name == "check" }.configureEach(object : Action<Task> {
            override fun execute(task: Task) {
                task.dependsOn(strictCheck)
            }
        })
    }

    private fun Configuration.isAndroidMainClasspath(): Boolean {
        if (!isCanBeResolved) return false
        val name = name.lowercase()
        val mainClasspath = name.endsWith("compileclasspath") ||
            name.endsWith("runtimeclasspath")
        val testClasspath = name.contains("test")
        return mainClasspath && !testClasspath
    }

    private fun Configuration.isAndroidMainDependencyBucket(): Boolean {
        if (isCanBeResolved || isCanBeConsumed) return false

        val name = name.lowercase()
        if (name.contains("test")) return false

        return name == "api" ||
            name == "implementation" ||
            name == "compileonly" ||
            name == "runtimeonly" ||
            name.endsWith("api") ||
            name.endsWith("implementation") ||
            name.endsWith("compileonly") ||
            name.endsWith("runtimeonly")
    }
}
