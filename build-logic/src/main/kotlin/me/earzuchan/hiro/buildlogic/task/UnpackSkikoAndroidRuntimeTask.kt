package me.earzuchan.hiro.buildlogic.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class UnpackSkikoAndroidRuntimeTask : DefaultTask() {
    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val arm64Runtime: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val x64Runtime: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun run() {
        val output = outputDirectory.get().asFile
        fileSystemOperations.delete { delete(output) }
        fileSystemOperations.copy {
            into(output)

            from(arm64Runtime.files.map { archiveOperations.zipTree(it) }) {
                include("libskiko-android-arm64.so")
                into("arm64-v8a")
            }

            from(x64Runtime.files.map { archiveOperations.zipTree(it) }) {
                include("libskiko-android-x64.so")
                into("x86_64")
            }
        }
    }
}
