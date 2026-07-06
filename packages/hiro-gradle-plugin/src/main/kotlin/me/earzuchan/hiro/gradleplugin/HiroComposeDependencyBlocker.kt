package me.earzuchan.hiro.gradleplugin

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.DirectDependenciesMetadata
import org.gradle.api.artifacts.ExternalModuleDependency

internal object HiroComposeDependencyBlocker {
    fun rejectDirectComposeDependencies(configuration: Configuration) {
        configuration.withDependencies(object : Action<DependencySet> {
            override fun execute(dependencies: DependencySet) {
                val composeDependencies = dependencies
                    .filterIsInstance<ExternalModuleDependency>()
                    .filter { dependency ->
                        HiroDependencyPolicy.isComposeLibrary(dependency.group.orEmpty())
                    }

                if (composeDependencies.isEmpty()) return

                throw GradleException(
                    buildString {
                        appendLine("Hiro 不接受用户在 Android 依赖桶里直接声明官方 Compose 坐标。")
                        appendLine("请改用 me.earzuchan.hiro:hiro，Material3 另加 me.earzuchan.hiro:material3。")
                        composeDependencies.forEach { dependency ->
                            appendLine(" - ${dependency.group}:${dependency.name}:${dependency.version ?: "未指定版本"}")
                        }
                    },
                )
            }
        })
    }

    fun removeComposeDependencies(dependencies: DirectDependenciesMetadata) {
        val iterator = dependencies.iterator()
        while (iterator.hasNext()) {
            val dependency = iterator.next()
            if (HiroDependencyPolicy.isComposeLibrary(dependency.group)) {
                iterator.remove()
            }
        }
    }
}
