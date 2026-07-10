package me.earzuchan.hiro.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

abstract class HiroJarProcessTask : DefaultTask() {
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

        logger.lifecycle("开始为${project}处理${output.name}")

        BlackMambaElbowing.mergeArchives(inputArtifacts.files, output, HiroBlackMambaProcessRule(dropPathPrefixes.get(), dropPathFragments.get(), dropBinaryPatterns.get()))

        BlackMambaElbowing.assertJar(output, requiredJarEntries.get(), forbiddenJarEntryFragments.get())
    }
}

object BlackMambaElbowing {
    // 处理

    fun mergeArchives(inputArchives: Iterable<File>, outputJar: File, rule: HiroBlackMambaProcessRule) {
        outputJar.parentFile.mkdirs()

        val seenEntries = linkedSetOf<String>()
        ZipOutputStream(BufferedOutputStream(outputJar.outputStream())).use { writer ->
            val writeEntry = fun(entryName: String, bytes: ByteArray) {
                if (entryName in seenEntries || shouldDropResource(entryName)) return
                if (shouldDropEntry(entryName, bytes, rule)) return

                seenEntries += entryName
                writer.putNextEntry(ZipEntry(entryName).apply { time = 0L })
                writer.write(bytes)
                writer.closeEntry()
            }

            inputArchives.filter { it.isFile }.sortedWith(compareBy(File::getName, File::getAbsolutePath)).forEach { input ->
                when (input.extension.lowercase()) {
                    "jar" -> mergeJar(input, writeEntry)
                    "aar" -> mergeAar(input, writeEntry)
                    else -> error("Hiro 不支持处理此归档类型：${input.absolutePath}")
                }
            }
        }
    }

    private fun mergeJar(inputJar: File, writeEntry: (String, ByteArray) -> Unit) = ZipFile(inputJar).use { jar ->
        jar.entries().asSequence().filterNot { it.isDirectory }.sortedBy { it.name }.forEach { entry -> writeEntry(entry.name, jar.getInputStream(entry).use { it.readBytes() }) }
    }

    private fun mergeAar(inputAar: File, writeEntry: (String, ByteArray) -> Unit) = ZipFile(inputAar).use { aar ->
        val embeddedJars = aar.entries().asSequence().filterNot { it.isDirectory }.filter { it.name == "classes.jar" || it.name.startsWith("libs/") && it.name.endsWith(".jar") }.sortedWith(compareBy({ it.name != "classes.jar" }, { it.name })).toList()

        check(embeddedJars.any { it.name == "classes.jar" }) { "Hiro 处理的 AAR 缺少 classes.jar：${inputAar.absolutePath}" }

        embeddedJars.forEach { embeddedJar ->
            val entries = ZipInputStream(ByteArrayInputStream(aar.getInputStream(embeddedJar).use { it.readBytes() })).use { jar ->
                buildList {
                    var entry = jar.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) add(entry.name to jar.readBytes())
                        jar.closeEntry()
                        entry = jar.nextEntry
                    }
                }
            }

            entries.sortedBy { it.first }.forEach { (entryName, bytes) -> writeEntry(entryName, bytes) }
        }

        aar.entries().asSequence().filterNot { it.isDirectory }.filter { it.name.startsWith("META-INF/") && !it.name.startsWith("META-INF/com/android/build/gradle/") }.sortedBy { it.name }.forEach { entry ->
            writeEntry(entry.name, aar.getInputStream(entry).use { it.readBytes() })
        }
    }

    // 校验

    fun assertJar(inputJar: File, requiredEntries: List<String>, forbiddenEntryFragments: List<String>) = ZipFile(inputJar).use { zip ->
        val entryNames = zip.entries().asSequence().map { it.name }.toSet()
        val missingEntries = requiredEntries.filterNot { it in entryNames }
        check(missingEntries.isEmpty()) { "Hiro 处理后的 jar 缺少必要条目：${missingEntries.joinToString()}" }

        val leak = entryNames.firstOrNull { entryName -> forbiddenEntryFragments.any { fragment -> entryName.contains(fragment) } }
        check(leak == null) { "Hiro 处理后的 jar 包含禁止路径：$leak" }
    }

    private fun shouldDropEntry(entryName: String, bytes: ByteArray, rule: HiroBlackMambaProcessRule): Boolean {
        if (rule.dropPathPrefixes.any { entryName.startsWith(it) }) return true
        if (rule.dropPathFragments.any { entryName.contains(it) }) return true
        if (!entryName.endsWith(".class")) return false

        val content = bytes.toString(Charsets.ISO_8859_1)

        return rule.dropBinaryPatterns.any { pattern -> entryName.contains(pattern) || content.contains(pattern) }
    }

    private fun shouldDropResource(entryName: String): Boolean {
        val upperName = entryName.uppercase()
        return upperName == "META-INF/MANIFEST.MF" || upperName.endsWith(".SF") || upperName.endsWith(".DSA") || upperName.endsWith(".RSA")
    }
}

data class HiroBlackMambaProcessRule(val dropPathPrefixes: List<String>, val dropPathFragments: List<String>, val dropBinaryPatterns: List<String>)
