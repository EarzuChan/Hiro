package me.earzuchan.hiro.skia

import android.content.Context
import android.os.Process
import org.jetbrains.skiko.Library
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

object HiroSkiaRuntime {
    private const val ICU_DATA_ASSET = "blackmamba.icu"
    private const val ICU_DATA_SIZE = 778_864
    private const val ICU_DATA_CACHE_KEY = "c12537022ef818991a7bfed41a76d8d6ae962ffbc0e6511ac762a5d0845e7f7c"
    private const val COPY_BUFFER_SIZE = 64 * 1024
    private const val CACHE_DIRECTORY = "hiro-icu"
    private const val CACHE_LOCK_FILE = "cache.lock"
    private const val CACHE_FILE_PREFIX = "blackmamba-"
    private const val CACHE_FILE_SUFFIX = ".icu"
    private const val CACHE_FILE = "$CACHE_FILE_PREFIX$ICU_DATA_CACHE_KEY$CACHE_FILE_SUFFIX"

    @Volatile
    private var initialized = false

    private var icuData: ByteBuffer? = null

    fun initialize(context: Context) {
        if (initialized) return

        synchronized(this) {
            if (initialized) return

            val data = icuData ?: loadIcuData(context.applicationContext).also { icuData = it }
            Library.load()
            val error = nativeInitializeIcu(data)
            check(error == null) { "ICU 初始化失败：$error" }
            initialized = true
        }
    }

    private fun loadIcuData(context: Context): ByteBuffer {
        val cacheDirectory = File(context.codeCacheDir, CACHE_DIRECTORY)
        check(cacheDirectory.isDirectory || cacheDirectory.mkdirs()) { "无法创建 ICU 缓存目录：${cacheDirectory.absolutePath}" }

        val dataFile = File(cacheDirectory, CACHE_FILE)
        val lockFile = File(cacheDirectory, CACHE_LOCK_FILE)

        RandomAccessFile(lockFile, "rw").channel.use { lockChannel ->
            lockChannel.lock().use {
                if (!isValidCachedData(dataFile)) extractIcuData(context, dataFile)
                deleteObsoleteCacheFiles(cacheDirectory, dataFile)
                return mapIcuData(dataFile)
            }
        }
    }

    private fun isValidCachedData(dataFile: File) = dataFile.isFile && dataFile.length() == ICU_DATA_SIZE.toLong() // 现有缓存只校验长度，避免每次启动顺序读取全部 data 而破坏按需分页

    private fun extractIcuData(context: Context, dataFile: File) {
        check(!dataFile.exists() || dataFile.delete()) { "无法删除无效的 ICU 缓存：${dataFile.absolutePath}" }

        val tempFile = File(dataFile.parentFile, "${dataFile.name}.${Process.myPid()}.tmp")
        check(!tempFile.exists() || tempFile.delete()) { "无法删除残留的 ICU 临时文件：${tempFile.absolutePath}" }

        try {
            val copyBuffer = ByteArray(COPY_BUFFER_SIZE)
            var copiedBytes = 0L

            FileOutputStream(tempFile).use { output ->
                context.assets.open(ICU_DATA_ASSET).use { input ->
                    while (true) {
                        val count = input.read(copyBuffer)
                        if (count < 0) break
                        copiedBytes += count
                        check(copiedBytes <= ICU_DATA_SIZE) { "ICU data 大于预期的 $ICU_DATA_SIZE 字节" }
                        output.write(copyBuffer, 0, count)
                    }
                }
            }

            check(copiedBytes == ICU_DATA_SIZE.toLong()) { "ICU data 大小错误：预期 $ICU_DATA_SIZE 字节，实际 $copiedBytes 字节" }

            check(tempFile.renameTo(dataFile)) { "无法发布 ICU 缓存：${dataFile.absolutePath}" }
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private fun deleteObsoleteCacheFiles(cacheDirectory: File, currentDataFile: File) = cacheDirectory.listFiles()?.forEach { file ->
        if (file != currentDataFile && file.name.startsWith(CACHE_FILE_PREFIX)) file.delete()
    }

    private fun mapIcuData(dataFile: File): ByteBuffer = RandomAccessFile(dataFile, "r").channel.use { return it.map(FileChannel.MapMode.READ_ONLY, 0, ICU_DATA_SIZE.toLong()) }

    private external fun nativeInitializeIcu(data: ByteBuffer): String?
}
