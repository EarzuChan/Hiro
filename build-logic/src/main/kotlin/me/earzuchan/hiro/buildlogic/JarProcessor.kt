package me.earzuchan.hiro.buildlogic

import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object JarProcessor {
    fun mergeJars(inputJars: Iterable<File>, outputJar: File, rule: HiroJarProcessRule) {
        outputJar.parentFile.mkdirs()

        val seenEntries = linkedSetOf<String>()
        ZipOutputStream(BufferedOutputStream(outputJar.outputStream())).use { writer ->
            inputJars.filter { it.isFile && it.extension == "jar" }.sortedBy { it.name }.forEach { input ->
                ZipFile(input).use { zip ->
                    val entries = zip.entries()

                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.isDirectory) continue

                        val entryName = entry.name
                        if (entryName in seenEntries || shouldDropResource(entryName)) continue

                        val bytes = zip.getInputStream(entry).use { it.readBytes() }
                        if (shouldDropEntry(entryName, bytes, rule)) continue

                        seenEntries += entryName
                        writer.putNextEntry(ZipEntry(entryName).apply { time = 0L })
                        writer.write(bytes)
                        writer.closeEntry()
                    }
                }
            }
        }
    }

    fun assertJar(inputJar: File, requiredEntries: List<String>, forbiddenEntryFragments: List<String>) = ZipFile(inputJar).use { zip ->
        val entryNames = zip.entries().asSequence().map { it.name }.toSet()
        val missingEntries = requiredEntries.filterNot { it in entryNames }
        check(missingEntries.isEmpty()) { "Hiro 处理后的 jar 缺少必要条目：${missingEntries.joinToString()}" }

        val leak = entryNames.firstOrNull { entryName -> forbiddenEntryFragments.any { fragment -> entryName.contains(fragment) } }
        check(leak == null) { "Hiro 处理后的 jar 包含禁止路径：$leak" }
    }

    private fun shouldDropEntry(entryName: String, bytes: ByteArray, rule: HiroJarProcessRule): Boolean {
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

data class HiroJarProcessRule(val dropPathPrefixes: List<String>, val dropPathFragments: List<String>, val dropBinaryPatterns: List<String>)
