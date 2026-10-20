@file:JvmName("ResourceReader_desktopKt")
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package org.jetbrains.compose.resources

import java.io.FileNotFoundException
import java.io.InputStream
import me.earzuchan.hiro.compose.internal.HiroAndroidApplication

// TIPS：Compose 资源在 KMP 安卓目标中被打进 assets，在桌面与 JVM 目标中作为 Java 资源随 jar 分发，这里先查 assets 再查类加载器，两种来源都能读到

@ExperimentalResourceApi
internal fun getPlatformResourceReader(): ResourceReader = HiroAndroidResourceReader

@ExperimentalResourceApi
internal object HiroAndroidResourceReader : ResourceReader {
    private val classLoader: ClassLoader get() = HiroAndroidResourceReader::class.java.classLoader ?: error("找不到类加载器")

    override suspend fun read(path: String): ByteArray = open(path).use { it.readBytes() }

    override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray = open(path).use { it.readPart(offset, size) }

    override fun getUri(path: String): String {
        if (openAsset(path)?.use { true } == true) return "file:///android_asset/$path"

        return (classLoader.getResource(path) ?: throw MissingResourceException(path)).toURI().toString()
    }

    private fun open(path: String): InputStream = openAsset(path) ?: classLoader.getResourceAsStream(path) ?: throw MissingResourceException(path)

    private fun openAsset(path: String): InputStream? {
        val assets = HiroAndroidApplication.contextOrNull?.assets ?: return null
        return try {
            assets.open(path)
        } catch (_: FileNotFoundException) {
            null
        }
    }
}

/** 与上游同名同义，按指定类加载器读取资源；读取片段时不依赖安卓 33 才有的 InputStream.readNBytes */
@ExperimentalResourceApi
class JvmResourceReader(private val classLoader: ClassLoader) : ResourceReader {
    companion object {
        // TIPS：上游按其模块名修饰后的访问器名调用，这里须保持一致
        @get:JvmName("getDefault\$library")
        internal val Default = JvmResourceReader(JvmResourceReader::class.java.classLoader!!)
    }

    override suspend fun read(path: String): ByteArray = getResourceAsStream(path).use { it.readBytes() }

    override suspend fun readPart(path: String, offset: Long, size: Long): ByteArray = getResourceAsStream(path).use { it.readPart(offset, size) }

    override fun getUri(path: String): String = (classLoader.getResource(path) ?: throw MissingResourceException(path)).toURI().toString()

    private fun getResourceAsStream(path: String): InputStream = classLoader.getResourceAsStream(path) ?: throw MissingResourceException(path)
}

private fun InputStream.readPart(offset: Long, size: Long): ByteArray {
    var skipped = 0L
    while (skipped < offset) {
        val count = skip(offset - skipped)
        if (count <= 0L) break
        skipped += count
    }

    val result = ByteArray(size.toInt())
    var read = 0
    while (read < result.size) {
        val count = read(result, read, result.size - read)
        if (count <= 0) break
        read += count
    }
    return result
}
