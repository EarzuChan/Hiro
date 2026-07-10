package me.earzuchan.hiro.compose.savable

import androidx.savedstate.serialization.SavedStateConfiguration
import kotlin.reflect.KClass

/** Hiro Compose 单个 Scene 使用的不可变 SavableState 编解码配置 */
class HiroSavableStateConfiguration private constructor(val kotlinSerializationConfiguration: SavedStateConfiguration, private val codecsByType: Map<KClass<*>, HiroSavableStateCodec<*>>, private val codecsById: Map<String, HiroSavableStateCodec<*>>) {
    /** 构造一个新的不可变配置 */
    class Builder {
        var kotlinSerializationConfiguration: SavedStateConfiguration = SavedStateConfiguration.DEFAULT

        private val codecsByType = linkedMapOf<KClass<*>, HiroSavableStateCodec<*>>()
        private val codecsById = linkedMapOf<String, HiroSavableStateCodec<*>>()

        /** 注册一个精确类型 Codec，同一类型和同一 typeId 均只允许注册一次 */
        fun addCodec(codec: HiroSavableStateCodec<*>): Builder = apply {
            require(codec.typeId.isNotBlank()) { "Hiro SavableState Codec 的 typeId 不能为空" }
            require(codec.type !in codecsByType) { "Hiro SavableState 类型重复注册：${codec.type.qualifiedName}" }
            require(codec.typeId !in codecsById) { "Hiro SavableState typeId 重复注册：${codec.typeId}" }

            codecsByType[codec.type] = codec
            codecsById[codec.typeId] = codec
        }

        fun build() = HiroSavableStateConfiguration(
            kotlinSerializationConfiguration = kotlinSerializationConfiguration,
            codecsByType = codecsByType.toMap(),
            codecsById = codecsById.toMap(),
        )
    }

    internal fun codecFor(type: KClass<*>) = codecsByType[type]

    internal fun codecFor(typeId: String) = codecsById[typeId]

    companion object {
        @JvmField
        val DEFAULT = Builder().build()
    }
}

/** 以 DSL 创建一个新的 [HiroSavableStateConfiguration] */
fun hiroSavableStateConfiguration(builderAction: HiroSavableStateConfiguration.Builder.() -> Unit) = HiroSavableStateConfiguration.Builder().apply(builderAction).build()
