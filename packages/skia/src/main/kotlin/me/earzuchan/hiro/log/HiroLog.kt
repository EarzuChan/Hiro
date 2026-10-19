package me.earzuchan.hiro.log

import android.util.Log

/**
 * Hiro 各模块统一使用的日志入口
 *
 * 消息以 lambda 传入，未达门槛时不会拼接字符串。各等级的判定拆成独立方法，便于 consumer-rules 让 R8 在压缩构建中整段移除
 */
@HiroInternalApi
object HiroLog {
    fun isVerboseLoggable() = HiroLogging.effectiveLevel <= HiroLogLevel.Verbose

    fun isDebugLoggable() = HiroLogging.effectiveLevel <= HiroLogLevel.Debug

    fun isInfoLoggable() = HiroLogging.effectiveLevel <= HiroLogLevel.Info

    fun isWarnLoggable() = HiroLogging.effectiveLevel <= HiroLogLevel.Warn

    fun isErrorLoggable() = HiroLogging.effectiveLevel <= HiroLogLevel.Error

    /** 逐事件、逐帧等高频日志 */
    inline fun v(tag: String, message: () -> String) {
        if (isVerboseLoggable()) Log.v(tag, message())
    }

    /** 生命周期等调试日志 */
    inline fun d(tag: String, message: () -> String) {
        if (isDebugLoggable()) Log.d(tag, message())
    }

    inline fun i(tag: String, message: () -> String) {
        if (isInfoLoggable()) Log.i(tag, message())
    }

    inline fun w(tag: String, throwable: Throwable? = null, message: () -> String) {
        if (isWarnLoggable()) Log.w(tag, message(), throwable)
    }

    inline fun e(tag: String, throwable: Throwable? = null, message: () -> String) {
        if (isErrorLoggable()) Log.e(tag, message(), throwable)
    }
}
