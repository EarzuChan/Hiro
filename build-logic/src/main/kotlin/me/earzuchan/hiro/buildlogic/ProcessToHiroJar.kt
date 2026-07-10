package me.earzuchan.hiro.buildlogic

import org.gradle.api.Buildable
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskDependency
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

class HiroBuildLogicPlugin : Plugin<Project> {
    override fun apply(project: Project) = Unit
}

class HiroArtifactProcessingSpec internal constructor(logicalName: String) {
    internal val coordinates = mutableListOf<String>() // 来源包们的坐标

    // 处理

    internal val dropPathPrefixes = mutableListOf<String>()

    internal val dropPathFragments = mutableListOf<String>()

    internal val dropBinaryPatterns = mutableListOf<String>()

    // 校验

    internal val requiredJarEntries = mutableListOf<String>()
    internal val forbiddenJarEntryFragments = mutableListOf<String>()

    var outputFileName: String = "$logicalName.jar"

    var transitive: Boolean = false

    fun artifact(coordinate: String) {
        coordinates += coordinate
    }

    fun artifacts(vararg coordinates: String) {
        this.coordinates += coordinates
    }

    fun dropPathPrefix(vararg prefixes: String) {
        dropPathPrefixes += prefixes
    }

    fun dropPathFragment(vararg fragments: String) {
        dropPathFragments += fragments
    }

    fun dropBinaryPattern(vararg patterns: String) {
        dropBinaryPatterns += patterns
    }

    fun requireJarEntry(vararg entries: String) {
        requiredJarEntries += entries
    }

    fun forbidJarEntryFragment(vararg fragments: String) {
        forbiddenJarEntryFragments += fragments
    }
}

class HiroProcessedJar internal constructor(val files: FileCollection, private val task: TaskProvider<HiroJarProcessTask>) : Buildable {
    override fun getBuildDependencies(): TaskDependency = TaskDependency { _: Task? -> setOf(task.get()) }
}

fun Project.processToHiroJar(name: String, configure: HiroArtifactProcessingSpec.() -> Unit): HiroProcessedJar {
    val spec = HiroArtifactProcessingSpec(name).apply(configure) // 规格
    val rawArtifacts = createHiroRawArtifactConfiguration(name, spec) // 创建工件分桶

    val task = tasks.register<HiroJarProcessTask>("process${name.toTaskNamePart()}HiroJar") {
        inputArtifacts.from(rawArtifacts)
        outputJar.set(layout.buildDirectory.file("generated/hiro/${spec.outputFileName}"))

        // 从规格提取要求

        dropPathPrefixes.set(spec.dropPathPrefixes) // 条目的路径以设定的任意前缀开头，则直接丢弃
        dropPathFragments.set(spec.dropPathFragments) // 如果条目的路径中包含设定的任意字符串片段，则直接丢弃
        dropBinaryPatterns.set(spec.dropBinaryPatterns) // 二进制内容：深度解析丢弃

        requiredJarEntries.set(spec.requiredJarEntries) // 必要条目校验：有任何缺失将抛出，防止核心被误删
        forbiddenJarEntryFragments.set(spec.forbiddenJarEntryFragments) // 禁止路径校验：检查生成的 JAR 中是否包含任何匹配的条目
    }

    // TODO：只有一个Jar，为什么要叫Files呢？能不能化简。。。？
    val files = files(task.flatMap { it.outputJar }).builtBy(task)
    return HiroProcessedJar(files = files, task = task)
}

private fun Project.createHiroRawArtifactConfiguration(name: String, spec: HiroArtifactProcessingSpec): NamedDomainObjectProvider<Configuration> {
    val configurationName = "hiro${name.toTaskNamePart()}RawArtifacts"

    val configuration = configurations.register(configurationName) {
        isCanBeConsumed = false
        isCanBeResolved = true
        isTransitive = spec.transitive // 可传递性
    }

    spec.coordinates.forEach { coordinate -> dependencies.add(configurationName, coordinate) }

    return configuration
}

private fun String.toTaskNamePart(): String = split('-', '_', '.', ':').filter { it.isNotBlank() }.joinToString(separator = "") { part -> part.replaceFirstChar { char -> char.uppercaseChar() } }
