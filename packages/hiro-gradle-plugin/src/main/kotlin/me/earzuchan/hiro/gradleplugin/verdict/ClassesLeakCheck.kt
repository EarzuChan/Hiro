package me.earzuchan.hiro.gradleplugin.verdict

import me.earzuchan.hiro.gradleplugin.HiroBinaryLeakAction
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipFile
import kotlin.io.path.createTempFile

private class ClassesLeakScanner {
    private val forbiddenPatterns = listOf(
        ForbiddenPattern(
            label = "Android Compose 原后端",
            binaryPatterns = listOf(
                "androidx/compose/ui/platform/AndroidComposeView",
                "androidx/compose/ui/platform/AndroidOwner",
                "androidx/compose/ui/platform/AndroidUiDispatcher",
                "androidx/compose/ui/platform/RenderNodeLayer",
                "androidx/compose/ui/platform/ViewLayer",
                "androidx/compose/ui/graphics/AndroidCanvas",
                "androidx/compose/ui/graphics/AndroidPaint",
            ),
        ),
        ForbiddenPattern(
            label = "Android AGSL / RenderEffect 原后端",
            binaryPatterns = listOf(
                "android/graphics/RuntimeShader",
                "android/graphics/RenderEffect",
            ),
        ),
        ForbiddenPattern(
            label = "Desktop 窗口后端",
            binaryPatterns = listOf(
                "java/awt/",
                "javax/swing/",
                "javafx/",
                "androidx/compose/ui/awt/",
                "org/jetbrains/skiko/awt/",
            ),
        ),
    )

    fun scanArtifact(file: File, owner: String) = when {
        file.isDirectory -> scanDirectory(file, owner)
        file.extension.equals("jar", ignoreCase = true) -> scanZip(file, owner)
        file.extension.equals("aar", ignoreCase = true) -> scanAar(file, owner)
        else -> emptyList()
    }

    private fun scanDirectory(directory: File, owner: String): List<String> {
        val leaks = mutableListOf<String>()
        val root = directory.toPath()

        directory.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { classFile ->
            val classPath = root.relativize(classFile.toPath()).toString().replace(File.separatorChar, '/')
            leaks += scanClass(owner, classPath) { classFile.readBytes() }
        }

        return leaks
    }

    private fun scanAar(file: File, owner: String): List<String> {
        val leaks = mutableListOf<String>()

        ZipFile(file).use { aar ->
            val entries = aar.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory) continue
                val isClassesJar = entry.name == "classes.jar"
                val isBundledJar = entry.name.startsWith("libs/") && entry.name.endsWith(".jar")

                if (!isClassesJar && !isBundledJar) continue

                val bytes = aar.getInputStream(entry).use { it.readBytes() }
                leaks += scanZipBytes(bytes, "$owner / ${entry.name}")
            }
        }

        return leaks
    }

    private fun scanZip(file: File, owner: String) = ZipFile(file).use { zip -> scanZip(zip, owner) }

    private fun scanZipBytes(bytes: ByteArray, owner: String): List<String> {
        val temporaryFile = createTempFile(prefix = "hiro-binary-scan-", suffix = ".jar").toFile()

        try {
            temporaryFile.writeBytes(bytes)
            return scanZip(temporaryFile, owner)
        } finally {
            temporaryFile.delete()
        }
    }

    private fun scanZip(zip: ZipFile, owner: String): List<String> {
        val leaks = mutableListOf<String>()
        val entries = zip.entries()

        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.isDirectory || !entry.name.endsWith(".class")) continue

            leaks += scanClass(owner, entry.name) { zip.getInputStream(entry).use { it.readBytes() } }
        }

        return leaks
    }

    private fun scanClass(owner: String, classPath: String, bytes: () -> ByteArray): List<String> {
        val pathLeak = forbiddenPatterns.firstOrNull { forbidden -> forbidden.binaryPatterns.any { pattern -> classPath.contains(pattern) } }

        if (pathLeak != null) return listOf("$owner: $classPath 命中 ${pathLeak.label}")

        val content = bytes().toString(Charsets.ISO_8859_1)

        return forbiddenPatterns.mapNotNull { forbidden ->
            val reference = forbidden.binaryPatterns.firstNotNullOfOrNull { pattern -> content.firstForbiddenReference(pattern) }
            reference?.let { "$owner: $classPath 引用 $it，命中 ${forbidden.label}" }
        }
    }

    private fun String.firstForbiddenReference(pattern: String): String? {
        val cursor = indexOf(pattern)
        return if (cursor >= 0) binaryReferenceAt(cursor) else null
    }

    private fun String.binaryReferenceAt(start: Int): String {
        var end = start
        while (end < length && this[end].isBinaryReferenceChar()) end++
        return substring(start, end)
    }

    private fun Char.isBinaryReferenceChar(): Boolean = isLetterOrDigit() || this == '/' || this == '$' || this == '_' || this == '-'

    private data class ForbiddenPattern(val label: String, val binaryPatterns: List<String>)
}

internal abstract class HiroClassesLeakCheckTask : DefaultTask() {
    @get:Classpath
    abstract val jarArtifacts: ConfigurableFileCollection

    @get:Input
    abstract val configurationPath: Property<String>

    @get:Input
    abstract val binaryLeakAction: Property<HiroBinaryLeakAction>

    @TaskAction
    fun check() {
        val leaks = linkedSetOf<String>()
        val scanner = ClassesLeakScanner()
        val preferredAction = binaryLeakAction.get()

        // 现在还不支持Strip！！

        jarArtifacts.files.sortedBy { it.absolutePath }.forEach { file ->
            if (!file.exists()) throw GradleException("Hiro Gradle 插件：${configurationPath.get()} 的后端渗漏检查输入尚未生成：${file.absolutePath}")

            leaks += scanner.scanArtifact(file, file.run { name.ifBlank { absolutePath } })
        }

        if (leaks.isEmpty()) {
            logger.lifecycle("Hiro Gradle 插件：${configurationPath.get()} 二进制后端渗漏检查通过")
            return
        }

        val message = buildString {
            appendLine("Hiro Gradle 插件：${configurationPath.get()} 发现 Android/desktop 原后端二进制渗漏")
            leaks.take(80).forEach { appendLine(" - $it") }
            if (leaks.size > 80) appendLine(" - ... 另有 ${leaks.size - 80} 条")
        }

        when (preferredAction) {
            HiroBinaryLeakAction.Fail -> throw GradleException(message)
            HiroBinaryLeakAction.Warn -> logger.warn(message)
        }
    }
}
