package me.earzuchan.hiro.gradleplugin.task

import me.earzuchan.hiro.gradleplugin.diagnostic.HiroDependencyLeakScanner
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class HiroStrictDependencyCheckTask : DefaultTask() {
    @get:Classpath
    abstract val artifactFiles: ConfigurableFileCollection

    @get:Input
    var strict: Boolean = true

    @TaskAction
    fun checkDependencies() {
        if (!strict) {
            logger.lifecycle("Hiro Gradle 插件 > 严格模式：已关闭，跳过依赖泄漏扫描")

            return
        }

        val leaks = linkedSetOf<String>()
        val scanner = HiroDependencyLeakScanner()
        val artifacts = artifactFiles.files

        logger.lifecycle("Hiro Gradle 插件 > 严格模式：开始扫描 ${artifacts.size} 个工件")

        artifacts.forEach { artifact -> leaks += scanner.scanArtifact(artifact, artifact.name) }

        if (leaks.isNotEmpty()) {
            logger.warn("Hiro Gradle 插件 > 严格模式：发现 ${leaks.size} 个不可接受路径")

            throw GradleException(buildString {
                appendLine("Hiro Gradle 插件 > 严格模式 检查失败：Android 依赖图仍有不可接受路径")
                leaks.forEach { appendLine(" - $it") }
            })
        }

        logger.lifecycle("Hiro Gradle 插件 > 严格模式：扫描通过，未发现 Android Compose / AGSL / 桌面窗口系统泄漏")
    }
}
