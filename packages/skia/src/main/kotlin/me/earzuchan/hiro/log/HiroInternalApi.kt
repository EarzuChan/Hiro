package me.earzuchan.hiro.log

/** 仅供 Hiro 各模块之间使用的接口，不保证兼容性，用户请勿调用 */
@RequiresOptIn(message = "这是 Hiro 内部接口，不保证兼容性", level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class HiroInternalApi
