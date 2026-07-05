package me.earzuchan.hiro.gradle.diagnostic

import java.io.File
import java.util.zip.ZipFile

internal class HiroDependencyLeakScanner {
    private val forbiddenDependencies = listOf(
        HiroForbiddenDependency(
            label = "Android Compose 后端",
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
        HiroForbiddenDependency(
            label = "Legacy Material",
            binaryPatterns = listOf(
                "androidx/compose/material/",
            ),
        ),
        HiroForbiddenDependency(
            label = "Android AGSL / RenderEffect",
            binaryPatterns = listOf(
                "android/graphics/RuntimeShader",
                "android/graphics/RenderEffect",
            ),
        ),
        HiroForbiddenDependency(
            label = "桌面窗口系统",
            binaryPatterns = listOf(
                "java/awt/",
                "javax/swing/",
                "javafx/",
                "androidx/compose/ui/awt/",
                "org/jetbrains/skiko/awt/",
            ),
        ),
    )

    fun scanArtifact(file: File, owner: String): List<String> =
        when {
            file.isDirectory -> scanDirectory(file, owner)
            file.extension.lowercase() == "jar" -> scanZip(file, owner)
            file.extension.lowercase() == "aar" -> scanAar(file, owner)
            else -> emptyList()
        }

    private fun scanDirectory(directory: File, owner: String): List<String> {
        val leaks = mutableListOf<String>()
        val root = directory.toPath()
        directory.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .forEach { classFile ->
                val classPath = root.relativize(classFile.toPath())
                    .toString()
                    .replace(File.separatorChar, '/')
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

    private fun scanZip(file: File, owner: String): List<String> =
        ZipFile(file).use { zip ->
            scanZip(zip, owner)
        }

    private fun scanZipBytes(bytes: ByteArray, owner: String): List<String> {
        val temporaryFile = kotlin.io.path.createTempFile(
            prefix = "hiro-strict-",
            suffix = ".jar",
        ).toFile()
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
            leaks += scanClass(owner, entry.name) {
                zip.getInputStream(entry).use { it.readBytes() }
            }
        }
        return leaks
    }

    private fun scanClass(
        owner: String,
        classPath: String,
        bytes: () -> ByteArray,
    ): List<String> {
        val pathLeak = forbiddenDependencies.firstOrNull { forbidden ->
            forbidden.binaryPatterns.any { pattern -> classPath.contains(pattern) }
        }
        if (pathLeak != null) {
            return listOf("$owner: $classPath 命中 ${pathLeak.label}")
        }

        val leaks = mutableListOf<String>()
        val content = bytes().toString(Charsets.ISO_8859_1)
        forbiddenDependencies.forEach { forbidden ->
            val pattern = forbidden.binaryPatterns.firstOrNull { content.contains(it) }
            if (pattern != null) {
                leaks += "$owner: $classPath 引用 $pattern，命中 ${forbidden.label}"
            }
        }
        return leaks
    }
}
