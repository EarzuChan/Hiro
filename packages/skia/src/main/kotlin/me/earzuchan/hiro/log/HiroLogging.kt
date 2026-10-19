package me.earzuchan.hiro.log

import android.content.Context
import android.content.pm.ApplicationInfo

/** Hiro 内部日志的等级，从低到高 */
enum class HiroLogLevel { Verbose, Debug, Info, Warn, Error, None }

/**
 * Hiro 日志的全局配置
 *
 * 宿主 App 不可调试（Release）时只输出 [HiroLogLevel.Warn] 与 [HiroLogLevel.Error]，[level] 只能把门槛调得更高
 * 可调试（Debug）时默认输出 [HiroLogLevel.Debug] 及以上，[level] 可任意调整（如调到 [HiroLogLevel.Verbose] 查看逐事件日志）
 * 压缩过的构建中，R8 会直接移除 Info 及以下等级的日志调用
 */
object HiroLogging {
    @Volatile
    private var debuggable = false

    @Volatile
    private var requestedLevel: HiroLogLevel? = null

    @Volatile
    private var threshold = HiroLogLevel.Warn

    /** 用户期望的日志门槛；为 null 时按宿主是否可调试取默认值 */
    var level: HiroLogLevel?
        get() = requestedLevel
        set(value) {
            requestedLevel = value
            updateThreshold()
        }

    /** 当前实际生效的日志门槛 */
    val effectiveLevel: HiroLogLevel get() = threshold

    @HiroInternalApi
    fun attach(context: Context) {
        val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebuggable == debuggable) return

        debuggable = isDebuggable
        updateThreshold()
    }

    private fun updateThreshold() {
        val requested = requestedLevel
        threshold = if (debuggable) requested ?: HiroLogLevel.Debug else maxOf(requested ?: HiroLogLevel.Warn, HiroLogLevel.Warn)
    }
}
