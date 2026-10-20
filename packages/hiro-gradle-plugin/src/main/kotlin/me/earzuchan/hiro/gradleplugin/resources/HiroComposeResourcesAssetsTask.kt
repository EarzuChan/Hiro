package me.earzuchan.hiro.gradleplugin.resources

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** 把处理后的 Compose 资源同步到 assets 的 composeResources/<包名>/ 下 */
abstract class HiroComposeResourcesAssetsTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val preparedDirectory: DirectoryProperty

    @get:Input
    abstract val packagePath: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @TaskAction
    fun pack() {
        fileSystem.sync { spec ->
            spec.from(preparedDirectory)
            spec.into(outputDirectory.dir(packagePath))
        }
    }
}
