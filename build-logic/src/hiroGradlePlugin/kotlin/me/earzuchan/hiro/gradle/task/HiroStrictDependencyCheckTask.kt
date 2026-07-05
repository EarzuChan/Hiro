package me.earzuchan.hiro.gradle.task

import me.earzuchan.hiro.gradle.diagnostic.HiroDependencyLeakScanner
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
        if (!strict) return

        val leaks = linkedSetOf<String>()
        val scanner = HiroDependencyLeakScanner()

        artifactFiles.files.forEach { artifact ->
            leaks += scanner.scanArtifact(artifact, artifact.name)
        }

        if (leaks.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Hiro strict 检查失败：Android 依赖图仍有不可接受路径")
                    leaks.forEach { appendLine(" - $it") }
                },
            )
        }
    }
}
