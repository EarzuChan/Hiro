package me.earzuchan.hiro.compose.internal.glue

import android.os.Bundle
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializerOrNull

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
internal class HiroKotlinSerializationSavableAdapter(
    private val configuration: SavedStateConfiguration,
    private val classLoader: ClassLoader,
) {
    private val serializerCache = mutableMapOf<Class<*>, KSerializer<Any>?>()

    fun canSerialize(value: Any) = serializerFor(value.javaClass) != null

    fun serialize(value: Any): Bundle {
        val serializer = checkNotNull(serializerFor(value.javaClass)) { "没有找到 ${value.javaClass.name} 的 Kotlin Serializer" }
        return encodeToSavedState(serializer, value, configuration)
    }

    fun deserialize(className: String, state: Bundle): Any {
        state.classLoader = classLoader
        val type = Class.forName(className, false, classLoader)
        val serializer = checkNotNull(serializerFor(type)) { "恢复 Hiro 状态时没有找到 $className 的 Kotlin Serializer" }
        return decodeFromSavedState(serializer, state, configuration)
    }

    @Suppress("UNCHECKED_CAST")
    private fun serializerFor(type: Class<*>): KSerializer<Any>? {
        if (type in serializerCache) return serializerCache[type]

        val kotlinType = type.kotlin
        val serializer = configuration.serializersModule.getContextual(kotlinType) ?: kotlinType.serializerOrNull()
        return (serializer as KSerializer<Any>?).also { serializerCache[type] = it }
    }
}
