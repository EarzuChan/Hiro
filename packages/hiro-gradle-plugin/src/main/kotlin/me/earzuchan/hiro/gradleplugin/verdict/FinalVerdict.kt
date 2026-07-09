package me.earzuchan.hiro.gradleplugin.verdict

import me.earzuchan.hiro.gradleplugin.HiroExtension
import me.earzuchan.hiro.gradleplugin.misc.HiroDependencyPolicy
import me.earzuchan.hiro.gradleplugin.processing.HiroVariantKind
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Attribute
import java.util.Collections

internal class HiroFinalVerdict(private val project: Project, private val extension: HiroExtension) {
    private val reportedHiroVariants = Collections.synchronizedSet(linkedSetOf<String>())
    private val reportedVariantConsistency = Collections.synchronizedSet(linkedSetOf<String>())

    private val variantSnapshotsLock = Any()

    private val variantSnapshots = linkedMapOf<String, MutableMap<ClasspathRole, Map<String, VariantSelection>>>()

    fun perform(configuration: Configuration) {
        attachBinaryLeakCheckTaskToAndroidLifecycle(configuration) // 这个在AfterResolve娶不到，必须挂任务

        configuration.incoming.afterResolve { // INCOMING：获取外部依赖的入口
            val selectedThirdPartyVariants = collectSelectedThirdPartyKmpVariants(configuration)

            reportKmpPackageSelectedHiroVariants(configuration, selectedThirdPartyVariants.values)

            checkComposeModulesOrJbrApiInClasspath(configuration)

            checkCompileRuntimeClasspathKmpVariantConsistency(configuration, selectedThirdPartyVariants)
        }
    }

    // 收集第三方KMP变体
    private fun collectSelectedThirdPartyKmpVariants(configuration: Configuration): Map<String, VariantSelection> {
        val selectedVariants = linkedMapOf<String, VariantSelection>()

        configuration.incoming.resolutionResult.allDependencies.filterIsInstance<ResolvedDependencyResult>().forEach { dependency ->
            val selectedModule = dependency.selected.moduleVersion ?: return@forEach
            if (!HiroDependencyPolicy.isThirdPartyKmpCandidate(selectedModule.group)) return@forEach

            val variant = dependency.resolvedVariant
            val variantName = variant.displayName
            val externalVariantName = variant.externalVariant.orElse(null)?.displayName
            val moduleKey = "${selectedModule.group}:${selectedModule.name}"

            selectedVariants[moduleKey] = VariantSelection(
                module = "$moduleKey:${selectedModule.version}",
                variantName = variantName,
                externalVariantName = externalVariantName,
                isHiroVariant = isHiroVariant(variantName, externalVariantName),
                kind = findHiroVariantKind(variantName, externalVariantName),
            )
        }

        return selectedVariants
    }

    // 报告选择的KMP包的Hiro特制变体
    private fun reportKmpPackageSelectedHiroVariants(configuration: Configuration, selectedVariants: Collection<VariantSelection>) = selectedVariants.filter { it.isHiroVariant }.forEach { selection ->
        val logKey = "${configuration.name}|${selection.module}|${selection.variantName}|${selection.externalVariantName}"

        if (reportedHiroVariants.add(logKey)) project.logger.lifecycle(buildString {
            append("Hiro Gradle 插件：${configuration.displayPath()} 选择了 ${selection.module} 的 Hiro特制变体：${selection.variantName}")
            selection.externalVariantName?.let { append("，外部变体：$it") }
        })
    }

    // 扫描在类路径在是否还有不想要的模块
    private fun checkComposeModulesOrJbrApiInClasspath(configuration: Configuration) {
        val leaks = configuration.incoming.resolutionResult.allDependencies.filterIsInstance<ResolvedDependencyResult>().mapNotNull { dependency ->
            val selectedModule = dependency.selected.moduleVersion ?: return@mapNotNull null // 解析不到版本，则或为断章，就跳过
            if (!HiroDependencyPolicy.isComposeModuleOrJbrApi(selectedModule.group, selectedModule.name)) return@mapNotNull null

            val from = dependency.from.moduleVersion?.let { "${it.group}:${it.name}:${it.version}" } ?: dependency.from.id.displayName

            " - $from -> ${dependency.requested.displayName} => ${selectedModule.group}:${selectedModule.name}:${selectedModule.version}"
        }.distinct()

        if (leaks.isEmpty()) {
            project.logger.lifecycle("Hiro Gradle 插件：检查通过，${configuration.displayPath()} 无官方 Compose 模块掺入 Classpath")
            return
        }

        throw GradleException(buildString {
            appendLine("Hiro Gradle 插件：${configuration.displayPath()} 发现官方 Compose 模块掺入 classpath")
            appendLine("请确保您没有引入官方 Compose 模块")
            appendLine()
            appendLine("掺入的情况：")

            leaks.take(80).forEach { appendLine(it) }
            if (leaks.size > 80) appendLine(" - ... 另有 ${leaks.size - 80} 条")
        })
    }

    private fun collectVariantMismatchMessages(compileVariants: Map<String, VariantSelection>, runtimeVariants: Map<String, VariantSelection>) = compileVariants.keys.intersect(runtimeVariants.keys).mapNotNull { moduleKey ->
        val compile = compileVariants.getValue(moduleKey)
        val runtime = runtimeVariants.getValue(moduleKey)

        when {
            compile.module != runtime.module -> "$moduleKey 版本不一致：编译期为 ${compile.module}，而运行期为 ${runtime.module}"

            compile.isHiroVariant != runtime.isHiroVariant -> "$moduleKey Hiro特制变体接管状态不一致：编译期为 ${compile.describe()}，而运行期为 ${runtime.describe()}"

            compile.isHiroVariant && compile.kind != runtime.kind -> "$moduleKey Hiro特制变体种类不一致：编译期为 ${compile.describe()}，而运行期为 ${runtime.describe()}"

            else -> null
        }
    }

    // 检查编译期运行期的KMP变体（尤其是Hiro变体）一致性
    private fun checkCompileRuntimeClasspathKmpVariantConsistency(configuration: Configuration, selectedThirdPartyVariants: Map<String, VariantSelection>) {
        val coordinates = configuration.classpathCoordinates() ?: return

        val pair = synchronized(variantSnapshotsLock) {
            val variantsByRole = variantSnapshots.getOrPut(coordinates.family) { linkedMapOf() }
            variantsByRole[coordinates.role] = selectedThirdPartyVariants

            val compile = variantsByRole[ClasspathRole.Compile]
            val runtime = variantsByRole[ClasspathRole.Runtime]
            if (compile != null && runtime != null) compile to runtime else null
        } ?: return

        val mismatches = collectVariantMismatchMessages(pair.first, pair.second)
        val reportKey = "${project.path}|${coordinates.family}"

        if (mismatches.isNotEmpty()) throw GradleException(buildString {
            appendLine("Hiro Gradle 插件：发现 ${project.path}:${coordinates.family} 编译期/运行期 类路径 的三方 KMP 变体不一致")
            appendLine()
            appendLine("情况：")
            mismatches.take(80).forEach { appendLine(" - $it") }
            if (mismatches.size > 80) appendLine(" - ... 另有 ${mismatches.size - 80} 条")
        })

        if (reportedVariantConsistency.add(reportKey)) {
            val commonCount = pair.first.keys.intersect(pair.second.keys).size

            project.logger.lifecycle("Hiro Gradle 插件：${project.path}:${coordinates.family} 编译期/运行期 classpath 三方 KMP 变体一致性检查通过，共同模块 $commonCount 个")
        }
    }

    // 辅助方法

    private fun attachBinaryLeakCheckTaskToAndroidLifecycle(configuration: Configuration) {
        val variantPrefix = configuration.androidVariantPrefix() ?: return
        val preVariantBuildTaskName = "pre${variantPrefix.capitalized()}Build" // 凑出Gradle既有任务

        val task = project.tasks.register("hiroCheck${configuration.name.capitalized()}ClassesLeaks", HiroClassesLeakCheckTask::class.java) { task ->
            task.group = "verification"
            task.description = "检查 ${configuration.displayPath()} 是否还有 Android/desktop 原后端二进制渗漏"

            task.configurationPath.set(configuration.displayPath())
            task.binaryLeakAction.convention(project.provider { extension.binaryLeakAction })
            task.jarArtifacts.from(configuration.incoming.artifactView { it.attributes.attribute(artifactType, "jar") }.files) // 预选仅Jar
        }

        project.tasks.configureEach { if (it.name == preVariantBuildTaskName) it.dependsOn(task) }
    }

    private fun Configuration.androidVariantPrefix() = when {
        name.endsWith("CompileClasspath") || name.endsWith("RuntimeClasspath") -> name.dropLast(16).ifBlank { null }
        else -> null
    }

    private fun Configuration.displayPath(): String = "${project.path}:$name"

    private fun Configuration.classpathCoordinates(): ClasspathCoordinates? = name.lowercase().let {
        when {
            it.endsWith("compileclasspath") -> ClasspathCoordinates(family = name.dropLast("compileClasspath".length).ifBlank { "main" }, role = ClasspathRole.Compile)

            it.endsWith("runtimeclasspath") -> ClasspathCoordinates(family = name.dropLast("runtimeClasspath".length).ifBlank { "main" }, role = ClasspathRole.Runtime)

            else -> null
        }
    }

    private fun isHiroVariant(variantName: String, externalVariantName: String?) = variantName.startsWith("hiro") || externalVariantName.orEmpty().startsWith("hiro")

    private fun findHiroVariantKind(variantName: String, externalVariantName: String?): String? {
        if (!isHiroVariant(variantName, externalVariantName)) return null

        val searchableName = "$variantName ${externalVariantName.orEmpty()}"
        return HiroVariantKind.priority.firstOrNull { kind -> searchableName.contains(kind.wireName, ignoreCase = true) }?.wireName
    }

    private fun String.capitalized(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    private enum class ClasspathRole { Compile, Runtime }

    private data class ClasspathCoordinates(val family: String, val role: ClasspathRole)

    private data class VariantSelection(val module: String, val variantName: String, val externalVariantName: String?, val isHiroVariant: Boolean, val kind: String?) {
        fun describe() = buildString {
            append(module)
            append(" / ")
            append(variantName)
            externalVariantName?.let { append(" / 外部变体 $it") }
            kind?.let { append(" / 种类 $it") }
        }
    }

    private companion object {
        val artifactType: Attribute<String> = Attribute.of("artifactType", String::class.java)
    }
}
