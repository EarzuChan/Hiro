package me.earzuchan.hiro.gradle

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.DirectDependenciesMetadata
import org.gradle.api.artifacts.DirectDependencyMetadata
import org.gradle.api.artifacts.ExternalModuleDependency

internal object HiroDependencyRewriter {
    fun rewriteDirectDependencies(
        project: Project,
        configuration: Configuration,
        selfPackageProject: Boolean,
    ) {
        if (selfPackageProject) return

        configuration.withDependencies(object : Action<DependencySet> {
            override fun execute(dependencies: DependencySet) {
                val replacements = dependencies
                    .filterIsInstance<ExternalModuleDependency>()
                    .mapNotNull { dependency ->
                        val target = HiroDependencyPolicy.replacementFor(
                            group = dependency.group.orEmpty(),
                            module = dependency.name,
                        ) ?: return@mapNotNull null

                        dependency to project.dependencies.create(target.notation).also { created: Dependency ->
                            if (created is ExternalModuleDependency) {
                                created.isTransitive = dependency.isTransitive
                                dependency.excludeRules.forEach { rule ->
                                    val exclude = mutableMapOf<String, String>()
                                    rule.group?.let { exclude["group"] = it }
                                    rule.module?.let { exclude["module"] = it }
                                    created.exclude(exclude)
                                }
                            }
                        }
                    }

                replacements.forEach { (original, replacement) ->
                    dependencies.remove(original)
                    dependencies.add(replacement)
                }
            }
        })
    }

    fun rewriteMetadataDependencies(
        ownerGroup: String,
        dependencies: DirectDependenciesMetadata,
    ) {
        if (HiroDependencyPolicy.isHiroModule(ownerGroup)) return
        if (HiroDependencyPolicy.isComposeMultiplatformModule(ownerGroup)) return

        val replacements = dependencies.mapNotNull { dependency ->
            val target = HiroDependencyPolicy.replacementFor(
                group = dependency.group,
                module = dependency.name,
            ) ?: return@mapNotNull null

            dependency to target
        }

        replacements.forEach { (original, target) ->
            dependencies.remove(original)
            dependencies.add(
                target.notation,
                object : Action<DirectDependencyMetadata> {
                    override fun execute(replacement: DirectDependencyMetadata) {
                        original.reason?.let(replacement::because)
                        if (original.isEndorsingStrictVersions) {
                            replacement.endorseStrictVersions()
                        } else {
                            replacement.doNotEndorseStrictVersions()
                        }
                    }
                },
            )
        }
    }
}
