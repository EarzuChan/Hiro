package me.earzuchan.hiro.buildlogic

import org.gradle.api.Buildable
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskDependency
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

class HiroArtifactProcessingSpec internal constructor(logicalName: String) {
    internal val coordinates = mutableListOf<String>()
    internal val dropPathPrefixes = mutableListOf<String>()
    internal val dropPathFragments = mutableListOf<String>()
    internal val dropBinaryPatterns = mutableListOf<String>()
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

class HiroProcessedJar internal constructor(val files: FileCollection, val task: TaskProvider<HiroProcessJarTask>) : Buildable {
    override fun getBuildDependencies(): TaskDependency = TaskDependency { _: Task? -> setOf(task.get()) }
}

fun Project.hiroProcessedJar(name: String, configure: HiroArtifactProcessingSpec.() -> Unit): HiroProcessedJar {
    val spec = HiroArtifactProcessingSpec(name).apply(configure)
    val rawArtifacts = createHiroRawArtifactConfiguration(name, spec)

    val task = tasks.register<HiroProcessJarTask>("process${name.toTaskNamePart()}HiroJar") {
        inputArtifacts.from(rawArtifacts)
        outputJar.set(layout.buildDirectory.file("generated/hiro/$name/${spec.outputFileName}"))
        dropPathPrefixes.set(spec.dropPathPrefixes)
        dropPathFragments.set(spec.dropPathFragments)
        dropBinaryPatterns.set(spec.dropBinaryPatterns)
        requiredJarEntries.set(spec.requiredJarEntries)
        forbiddenJarEntryFragments.set(spec.forbiddenJarEntryFragments)
    }

    val files = files(task.flatMap { it.outputJar }).builtBy(task)
    return HiroProcessedJar(files = files, task = task)
}

private fun Project.createHiroRawArtifactConfiguration(name: String, spec: HiroArtifactProcessingSpec): org.gradle.api.NamedDomainObjectProvider<org.gradle.api.artifacts.Configuration> {
    val configurationName = "hiro${name.toTaskNamePart()}RawArtifacts"
    val configuration = configurations.register(configurationName) {
        isCanBeConsumed = false
        isCanBeResolved = true
        isTransitive = spec.transitive
    }

    spec.coordinates.forEach { coordinate -> dependencies.add(configurationName, coordinate) }

    return configuration
}

private fun String.toTaskNamePart(): String = split('-', '_', '.', ':').filter { it.isNotBlank() }.joinToString(separator = "") { part -> part.replaceFirstChar { char -> char.uppercaseChar() } }
