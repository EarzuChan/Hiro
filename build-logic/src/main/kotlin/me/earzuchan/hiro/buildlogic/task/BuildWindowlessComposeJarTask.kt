package me.earzuchan.hiro.buildlogic.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

abstract class BuildWindowlessComposeJarTask : DefaultTask() {
    @get:Classpath
    abstract val inputJars: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @get:Input
    abstract val forbiddenPathPrefixes: ListProperty<String>

    @get:Input
    abstract val forbiddenPathFragments: ListProperty<String>

    @get:Input
    abstract val forbiddenBinaryPatterns: ListProperty<String>

    @TaskAction
    fun run() {
        val output = outputJar.get().asFile
        output.parentFile.mkdirs()

        val seenEntries = linkedSetOf<String>()
        ZipOutputStream(BufferedOutputStream(output.outputStream())).use { writer ->
            inputJars.files
                .filter { it.isFile && it.extension == "jar" }
                .sortedBy { it.name }
                .forEach { input ->
                    ZipFile(input).use { zip ->
                        val entries = zip.entries()
                        while (entries.hasMoreElements()) {
                            val entry = entries.nextElement()
                            if (entry.isDirectory) continue
                            val entryName = entry.name
                            if (entryName in seenEntries || shouldDropResource(entryName)) continue

                            val bytes = zip.getInputStream(entry).use { it.readBytes() }
                            if (shouldDropEntry(entryName, bytes)) continue

                            seenEntries += entryName
                            writer.putNextEntry(
                                ZipEntry(entryName).apply {
                                    time = 0L
                                },
                            )
                            writer.write(bytes)
                            writer.closeEntry()
                        }
                    }
                }
        }
    }

    private fun shouldDropEntry(entryName: String, bytes: ByteArray): Boolean {
        if (forbiddenPathPrefixes.get().any { entryName.startsWith(it) }) return true
        if (forbiddenPathFragments.get().any { entryName.contains(it) }) return true
        if (!entryName.endsWith(".class")) return false

        val content = bytes.toString(Charsets.ISO_8859_1)
        return forbiddenBinaryPatterns.get().any { pattern ->
            entryName.contains(pattern) || content.contains(pattern)
        }
    }

    private fun shouldDropResource(entryName: String): Boolean {
        val upperName = entryName.uppercase()
        return upperName == "META-INF/MANIFEST.MF" ||
            upperName.endsWith(".SF") ||
            upperName.endsWith(".DSA") ||
            upperName.endsWith(".RSA")
    }
}
