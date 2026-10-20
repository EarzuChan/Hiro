@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package me.earzuchan.hiro.gradleplugin.resources

import com.android.build.api.variant.AndroidComponentsExtension
import java.io.File
import org.gradle.api.Project
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.CopyNonXmlValueResourcesTask
import org.jetbrains.compose.resources.GenerateActualResourceCollectorsTask
import org.jetbrains.compose.resources.GenerateResClassTask
import org.jetbrains.compose.resources.GenerateResourceAccessorsTask
import org.jetbrains.compose.resources.PrepareComposeResourcesTask
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.compose.resources.XmlValuesConverterTask
import org.jetbrains.compose.resources.getResourcePackage

// TIPS：Compose 插件只为 KMP 与 Kotlin/JVM 模块生成 Res，纯安卓模块（AGP 内置 Kotlin）没有可用的 main 源码集。这里借用其任务类，按单一 main 源码集接线，生成代码挂进各变体的 Kotlin 源码，处理后的资源放进 assets
internal object HiroComposeResources {
    private const val GEN_DIR = "generated/compose/resourceGenerator"
    private const val SOURCE_SET = "main"

    fun configure(project: Project) {
        if (project.pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform") || project.pluginManager.hasPlugin("org.jetbrains.kotlin.jvm")) return

        val composeExtension = project.extensions.getByType(ComposeExtension::class.java)
        val resourcesExtension = composeExtension.extensions.getByType(ResourcesExtension::class.java)
        val config = project.provider { resourcesExtension }

        val packageName = config.getResourcePackage(project)
        val resClassName = config.map { it.nameOfResClass }
        val makeAccessorsPublic = config.map { it.publicResClass }
        val packagingDir = packageName.map { File("composeResources/$it") }
        val shouldGenerate = config.map { it.generateResClass != ResourcesExtension.ResourceClassGeneration.Never }

        val layout = project.layout
        val userResourcesDir = layout.projectDirectory.dir("src/$SOURCE_SET/composeResources")
        val preparedDir = layout.buildDirectory.dir("$GEN_DIR/preparedResources/$SOURCE_SET/composeResources")

        val convertXmls = project.tasks.register("convertXmlValueResourcesForMain", XmlValuesConverterTask::class.java) { task ->
            task.fileSuffix.set(SOURCE_SET)
            task.originalResourcesDir.set(userResourcesDir)
            task.outputDir.set(preparedDir)
        }

        val copyNonXmls = project.tasks.register("copyNonXmlValueResourcesForMain", CopyNonXmlValueResourcesTask::class.java) { task ->
            task.originalResourcesDir.set(userResourcesDir)
            task.outputDir.set(preparedDir)
        }

        val prepare = project.tasks.register("prepareComposeResourcesTaskForMain", PrepareComposeResourcesTask::class.java) { task ->
            task.convertedXmls.set(convertXmls.map { it.realOutputFiles.get() })
            task.copiedNonXmls.set(copyNonXmls.map { it.realOutputFiles.get() })
            task.outputDir.set(preparedDir)
        }

        val resClass = project.tasks.register("generateComposeResClass", GenerateResClassTask::class.java) { task ->
            task.packageName.set(packageName)
            task.resClassName.set(resClassName)
            task.makeAccessorsPublic.set(makeAccessorsPublic)
            task.codeDir.set(layout.buildDirectory.dir("$GEN_DIR/kotlin/commonResClass"))
            task.packagingDir.set(packagingDir)
            task.onlyIf { shouldGenerate.get() }
        }

        val accessors = project.tasks.register("generateResourceAccessorsForMain", GenerateResourceAccessorsTask::class.java) { task ->
            task.packageName.set(packageName)
            task.resClassName.set(resClassName)
            task.sourceSetName.set(SOURCE_SET)
            task.makeAccessorsPublic.set(makeAccessorsPublic)
            task.resDir.set(prepare.flatMap { it.outputDir.asFile })
            task.codeDir.set(layout.buildDirectory.dir("$GEN_DIR/kotlin/${SOURCE_SET}ResourceAccessors"))
            task.disableResourceContentHashGeneration.set(false)
            task.packagingDir.set(packagingDir)
            task.onlyIf { shouldGenerate.get() }
        }

        val collectors = project.tasks.register("generateActualResourceCollectorsForMain", GenerateActualResourceCollectorsTask::class.java) { task ->
            task.packageName.set(packageName)
            task.resClassName.set(resClassName)
            task.makeAccessorsPublic.set(makeAccessorsPublic)
            task.useActualModifier.set(false)
            task.resourceAccessorDirs.from(accessors.map { it.codeDir })
            task.codeDir.set(layout.buildDirectory.dir("$GEN_DIR/kotlin/${SOURCE_SET}ResourceCollectors"))
            task.onlyIf { shouldGenerate.get() }
        }

        // 运行时按 composeResources/<包名>/ 读取，故把处理后的资源放到 assets 的对应子目录。路径只取决于包名，自定义 Res 类名（nameOfResClass）不受影响，已实测
        val assets = project.tasks.register("hiroPackComposeResourcesAssets", HiroComposeResourcesAssetsTask::class.java) { task ->
            task.preparedDirectory.set(prepare.flatMap { it.outputDir })
            task.packagePath.set(packagingDir.map { it.invariantSeparatorsPath })
        }

        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants(androidComponents.selector().all()) { variant ->
            variant.sources.kotlin?.addGeneratedSourceDirectory(resClass) { it.codeDir }
            variant.sources.kotlin?.addGeneratedSourceDirectory(accessors) { it.codeDir }
            variant.sources.kotlin?.addGeneratedSourceDirectory(collectors) { it.codeDir }
            variant.sources.assets?.addGeneratedSourceDirectory(assets) { it.outputDirectory }
        }

        project.logger.info("Hiro Gradle 插件：已为 ${project.path} 接管 Compose 资源生成")
    }
}
