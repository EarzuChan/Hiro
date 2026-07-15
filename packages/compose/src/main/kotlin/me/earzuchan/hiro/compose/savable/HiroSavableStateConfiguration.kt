package me.earzuchan.hiro.compose.savable

import androidx.savedstate.serialization.SavedStateConfiguration
import me.earzuchan.hiro.compose.HiroComposeConfigurationDsl
import kotlin.reflect.KClass

/** Hiro Compose 使用的不可变 SavableState 编解码配置 */
class HiroSavableStateConfiguration private constructor(val kotlinSerializationConfiguration: SavedStateConfiguration, private val seDesByType: Map<KClass<*>, HiroSavableStateSeDes<*>>, private val seDesByTypeName: Map<String, HiroSavableStateSeDes<*>>) {
    /** 构造一个新的不可变配置 */
    @HiroComposeConfigurationDsl
    class Builder internal constructor(base: HiroSavableStateConfiguration? = null) {
        var kotlinSerializationConfiguration: SavedStateConfiguration = base?.kotlinSerializationConfiguration ?: SavedStateConfiguration.DEFAULT

        private val seDesByType = linkedMapOf<KClass<*>, HiroSavableStateSeDes<*>>().apply { base?.let { putAll(it.seDesByType) } }
        private val seDesByTypeName = linkedMapOf<String, HiroSavableStateSeDes<*>>().apply { base?.let { putAll(it.seDesByTypeName) } }

        /** 为一个精确类型注册特制 SeDes，同一类型只允许注册一次 */
        fun addSeDes(seDes: HiroSavableStateSeDes<*>): Builder = apply {
            val typeName = seDes.type.java.name
            require(seDes.type !in seDesByType) { "Hiro SavableState SeDes 类型重复注册：$typeName" }
            require(typeName !in seDesByTypeName) { "Hiro SavableState SeDes 类名重复注册：$typeName" }

            seDesByType[seDes.type] = seDes
            seDesByTypeName[typeName] = seDes
        }

        internal fun build() = HiroSavableStateConfiguration(
            kotlinSerializationConfiguration = kotlinSerializationConfiguration,
            seDesByType = seDesByType.toMap(),
            seDesByTypeName = seDesByTypeName.toMap(),
        )
    }

    internal fun seDesFor(type: KClass<*>) = seDesByType[type]

    internal fun seDesFor(typeName: String) = seDesByTypeName[typeName]

    internal fun toBuilder() = Builder(this)

    companion object {
        @JvmField
        val DEFAULT = Builder().build()
    }
}

/** 以 DSL 创建一个新的 [HiroSavableStateConfiguration] */
fun hiroSavableStateConfiguration(builderAction: HiroSavableStateConfiguration.Builder.() -> Unit) = HiroSavableStateConfiguration.Builder().apply(builderAction).build()
