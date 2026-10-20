package me.earzuchan.hiro.gradleplugin.linking

import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** 对 Jar 做平台重新链接：不含桌面平台类型引用的 Jar 原样放行，不产生拷贝 */
@CacheableTransform
internal abstract class HiroPlatformLinkTransform : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    @get:Classpath
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.get().asFile

        when {
            input.isDirectory -> outputs.dir(input)

            !input.isFile || !input.hasClassNeedingLinking() -> outputs.file(input)

            else -> input.linkTo(outputs.file("hiro-linked-${input.name}"))
        }
    }

    private fun File.hasClassNeedingLinking() = ZipFile(this).use { zip ->
        zip.entries().asSequence().any { entry -> entry.isClassFile() && HiroPlatformLinker.needsLinking(zip.getInputStream(entry).use { it.readBytes() }) }
    }

    private fun File.linkTo(output: File) = ZipFile(this).use { zip ->
        ZipOutputStream(output.outputStream().buffered()).use { out ->
            zip.entries().asSequence().filterNot { it.isSignatureFile() }.forEach { entry ->
                val bytes = zip.getInputStream(entry).use { it.readBytes() }
                val linked = if (entry.isClassFile() && HiroPlatformLinker.needsLinking(bytes)) HiroPlatformLinker.link(bytes, "$name!${entry.name}") else bytes

                out.putNextEntry(ZipEntry(entry.name).apply { time = entry.time })
                out.write(linked)
                out.closeEntry()
            }
        }
    }

    private fun ZipEntry.isClassFile() = !isDirectory && name.endsWith(".class")

    // 改写后原签名必然失效，一并去掉
    private fun ZipEntry.isSignatureFile() = name.startsWith("META-INF/") && signatureSuffixes.any { name.endsWith(it, ignoreCase = true) }

    private companion object {
        val signatureSuffixes = listOf(".SF", ".RSA", ".DSA", ".EC")
    }
}
