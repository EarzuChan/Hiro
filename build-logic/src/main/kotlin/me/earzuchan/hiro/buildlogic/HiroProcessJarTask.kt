package me.earzuchan.hiro.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class HiroProcessJarTask : DefaultTask() {
    @get:Classpath
    abstract val inputArtifacts: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @get:Input
    abstract val dropPathPrefixes: ListProperty<String>

    @get:Input
    abstract val dropPathFragments: ListProperty<String>

    @get:Input
    abstract val dropBinaryPatterns: ListProperty<String>

    @get:Input
    abstract val requiredJarEntries: ListProperty<String>

    @get:Input
    abstract val forbiddenJarEntryFragments: ListProperty<String>

    init {
        dropPathPrefixes.convention(emptyList())
        dropPathFragments.convention(emptyList())
        dropBinaryPatterns.convention(emptyList())
        requiredJarEntries.convention(emptyList())
        forbiddenJarEntryFragments.convention(emptyList())
    }

    @TaskAction
    fun run() {
        val output = outputJar.get().asFile

        JarProcessor.mergeJars(inputArtifacts.files, output, HiroJarProcessRule(dropPathPrefixes.get(), dropPathFragments.get(), dropBinaryPatterns.get()))

        JarProcessor.assertJar(output, requiredJarEntries.get(), forbiddenJarEntryFragments.get())
    }
}
