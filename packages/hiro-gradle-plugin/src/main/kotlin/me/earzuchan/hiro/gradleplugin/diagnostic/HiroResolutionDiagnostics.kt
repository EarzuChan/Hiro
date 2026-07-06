package me.earzuchan.hiro.gradleplugin.diagnostic

import me.earzuchan.hiro.gradleplugin.HiroDependencyPolicy
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedDependencyResult

internal class HiroResolutionDiagnostics(private val project: Project, private val strict: () -> Boolean) {
    private val resolvedConfigurations = linkedMapOf<String, HiroResolvedConfiguration>()
    private val reportedMismatchKeys = linkedSetOf<String>()

    fun attach(configuration: Configuration) = configuration.incoming.afterResolve {
        val resolvedConfiguration = collect(configuration)
        resolvedConfigurations[configuration.name] = resolvedConfiguration

        project.logger.lifecycle("Hiro Gradle 插件：使 ${configuration.displayPath()} 使用 Skia 后端")
        resolvedConfiguration.records.forEach { record -> reportResolvedDependency(configuration, record) }
        reportClasspathMismatch(configuration)
    }

    private fun collect(configuration: Configuration) = HiroResolvedConfiguration(
        configuration.name, configuration.incoming.resolutionResult.allDependencies
            .filterIsInstance<ResolvedDependencyResult>()
            .mapNotNull { dependency -> dependency.toRecord() }
            .distinctBy { record -> "${record.configurationFamilyKey}|${record.selectedModule}|${record.variantName}|${record.externalVariantName}" }
    )

    private fun ResolvedDependencyResult.toRecord(): HiroResolvedDependencyRecord? {
        val moduleVersion = selected.moduleVersion ?: return null
        val group = moduleVersion.group
        if (!HiroDependencyPolicy.isThirdPartyHijackCandidate(group)) return null

        val externalVariant = resolvedVariant.externalVariant.orElse(null)
        val moduleName = moduleVersion.name

        return HiroResolvedDependencyRecord(
            group = group,
            name = moduleName,
            version = moduleVersion.version,
            requested = requested.displayName,
            selectedModule = "${moduleVersion.group}:${moduleVersion.name}:${moduleVersion.version}",
            configurationFamilyKey = "$group:${moduleName.removeKmpPlatformSuffix()}:${moduleVersion.version}",
            variantName = resolvedVariant.displayName,
            externalVariantName = externalVariant?.displayName,
            externalVariantOwner = externalVariant?.owner?.displayName,
        )
    }

    private fun reportResolvedDependency(configuration: Configuration, record: HiroResolvedDependencyRecord) {
        when {
            record.isHiroSkia -> {
                project.logger.lifecycle(
                    buildString {
                        append("Hiro Gradle 插件：${configuration.displayPath()} 已选择 ${record.selectedModule} 的 Skia 变体：${record.variantName}")
                        record.externalVariantName?.let { append("，外部变体：$it") }
                        record.externalVariantOwner?.let { append("，工件：$it") }
                    },
                )
            }

            record.isAndroidOriginal -> {
                project.logger.warn(
                    buildString {
                        append("Hiro Gradle 插件：${configuration.displayPath()} 仍选中 ${record.selectedModule} 的 Android 变体：${record.variantName}")
                        record.externalVariantName?.let { append("，外部变体：$it") }
                        record.externalVariantOwner?.let { append("，工件：$it") }
                    },
                )
            }
        }
    }

    private fun reportClasspathMismatch(configuration: Configuration) {
        val family = configuration.androidVariantFamily() ?: return
        val compileConfiguration = resolvedConfigurations["${family}CompileClasspath"]
        val runtimeConfiguration = resolvedConfigurations["${family}RuntimeClasspath"]
        if (compileConfiguration == null || runtimeConfiguration == null) return

        val compileBackends = compileConfiguration.backendByFamilyKey()
        val runtimeBackends = runtimeConfiguration.backendByFamilyKey()
        val keys = compileBackends.keys.intersect(runtimeBackends.keys)

        keys.forEach { key ->
            val compileBackend = compileBackends.getValue(key)
            val runtimeBackend = runtimeBackends.getValue(key)
            if (compileBackend.backend == runtimeBackend.backend) return@forEach

            val reportKey = "$family|$key|${compileBackend.backend}|${runtimeBackend.backend}"
            if (!reportedMismatchKeys.add(reportKey)) return@forEach

            val message = buildString {
                appendLine("Hiro Gradle 插件：${project.path}:$family 的 编译期/运行期 依赖后端不一致")
                appendLine(" - $key")
                appendLine("   编译期：${compileBackend.description}")
                append("   运行期：${runtimeBackend.description}")
            }

            if (strict()) throw GradleException(message)

            project.logger.warn(message)
        }
    }

    private fun HiroResolvedConfiguration.backendByFamilyKey(): Map<String, HiroResolvedBackend> = records.groupBy { record -> record.configurationFamilyKey }.mapValues { (_, records) -> records.toBackend() }

    private fun List<HiroResolvedDependencyRecord>.toBackend(): HiroResolvedBackend {
        val selected = firstOrNull { it.isAndroidOriginal } ?: firstOrNull { it.isHiroSkia } ?: first()

        return HiroResolvedBackend(
            backend = selected.backendName,
            description = "${selected.backendName}（${selected.selectedModule}，${selected.variantName}${selected.externalVariantName?.let { "，外部变体：$it" } ?: ""}）",
        )
    }

    private fun Configuration.displayPath(): String = "${project.path}:$name"

    private fun Configuration.androidVariantFamily(): String? =
        when {
            name.endsWith("CompileClasspath") -> name.removeSuffix("CompileClasspath")

            name.endsWith("RuntimeClasspath") -> name.removeSuffix("RuntimeClasspath")

            else -> null
        }

    private fun String.removeKmpPlatformSuffix() = removeSuffix("-android").removeSuffix("-desktop").removeSuffix("-jvm").removeSuffix("-skiko")

    private data class HiroResolvedConfiguration(val name: String, val records: List<HiroResolvedDependencyRecord>)

    private data class HiroResolvedDependencyRecord(val group: String, val name: String, val version: String, val requested: String, val selectedModule: String, val configurationFamilyKey: String, val variantName: String, val externalVariantName: String?, val externalVariantOwner: String?) {
        val isHiroSkia: Boolean = variantName.startsWith("hiroSkia") || externalVariantName.orEmpty().startsWith("hiroSkia")
        val isAndroidOriginal: Boolean = variantName.startsWith("android") || externalVariantName.orEmpty().startsWith("android")
        val backendName: String
            get() = when {
                isHiroSkia -> "Hiro Skia"
                isAndroidOriginal -> "Android"
                variantName.startsWith("desktop") -> "Desktop"
                variantName.startsWith("jvm") -> "JVM"
                variantName.startsWith("skiko") -> "Skiko"
                else -> "未知"
            }
    }

    private data class HiroResolvedBackend(val backend: String, val description: String)
}
