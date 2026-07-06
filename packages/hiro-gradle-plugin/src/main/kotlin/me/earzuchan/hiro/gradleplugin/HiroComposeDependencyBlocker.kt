package me.earzuchan.hiro.gradleplugin

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.DirectDependenciesMetadata
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.logging.Logging
import java.util.Collections

internal object HiroComposeDependencyBlocker {
    private val logger = Logging.getLogger(HiroComposeDependencyBlocker::class.java)
    private val loggedBlockedModules = Collections.synchronizedSet(linkedSetOf<String>())
    private val loggedRemovedDependencies = Collections.synchronizedSet(linkedSetOf<String>())

    fun rejectDirectComposeDependencies(configuration: Configuration) {
        configuration.withDependencies(object : Action<DependencySet> {
            override fun execute(dependencies: DependencySet) {
                val composeDependencies = dependencies
                    .filterIsInstance<ExternalModuleDependency>()
                    .filter { dependency ->
                        HiroDependencyPolicy.isComposeLibrary(dependency.group.orEmpty())
                    }

                if (composeDependencies.isEmpty()) return

                logger.error("Hiro Gradle 插件：用户在 ${configuration.name} 直接声明了官方 Compose 坐标，构建终止")

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

    fun removeComposeDependencies(dependencies: DirectDependenciesMetadata, owner: String) {
        val iterator = dependencies.iterator()
        while (iterator.hasNext()) {
            val dependency = iterator.next()
            if (HiroDependencyPolicy.isComposeLibrary(dependency.group)) {
                val removed = "$owner -> ${dependency.group}:${dependency.name}:${dependency.versionConstraint.displayName}"
                if (loggedRemovedDependencies.add(removed)) {
                    logger.lifecycle("Hiro Gradle 插件：从 $owner 移除官方 Compose 传递依赖 ${dependency.group}:${dependency.name}:${dependency.versionConstraint.displayName}")
                }
                iterator.remove()
            }
        }
    }

    fun logBlockedComposeModule(id: ModuleVersionIdentifier) {
        val module = "${id.group}:${id.name}:${id.version}"
        if (loggedBlockedModules.add(module)) {
            logger.lifecycle("Hiro Gradle 插件：阻断官方 Compose 模块 $module 进入 Hiro Android classpath")
        }
    }
}
