package me.earzuchan.hiro.buildlogic.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class CheckSkikoAndroidJniLibsTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDirectory: DirectoryProperty

    @TaskAction
    fun run() {
        val output = inputDirectory.get().asFile
        val requiredFiles = listOf(output.resolve("arm64-v8a/libskiko-android-arm64.so"), output.resolve("x86_64/libskiko-android-x64.so"))
        val missingFiles = requiredFiles.filterNot { it.isFile }

        check(missingFiles.isEmpty()) { "缺少 Skiko Android native runtime：${missingFiles.joinToString()}" }
    }
}
