package me.earzuchan.hiro.compose.internal.glue

import android.os.Binder
import android.os.Bundle
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import androidx.compose.runtime.MutableDoubleState
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.core.util.size
import me.earzuchan.hiro.compose.savable.HiroSavableStateCodec
import me.earzuchan.hiro.compose.savable.HiroSavableStateConfiguration
import java.io.Serializable
import java.util.IdentityHashMap
import java.util.LinkedHashMap

internal class HiroSavableStateBundleCodec(
    private val configuration: HiroSavableStateConfiguration,
    private val classLoader: ClassLoader,
) {
    private val kotlinSerialization = HiroKotlinSerializationSavableAdapter(
        configuration = configuration.kotlinSerializationConfiguration,
        classLoader = classLoader,
    )

    fun canBeSaved(value: Any?): Boolean = canBeSaved(value, IdentityHashMap())

    fun encodeRegistry(values: Map<String, List<Any?>>) = Bundle().apply {
        values.forEach { (key, entryValues) ->
            putParcelableArrayList(key, ArrayList(entryValues.map(::encodeValue)))
        }
    }

    fun decodeRegistry(bundle: Bundle): Map<String, List<Any?>> {
        bundle.classLoader = classLoader

        return buildMap {
            bundle.keySet().forEach { key ->
                val encodedValues = checkNotNull(bundle.getParcelableArrayList<Bundle>(key)) { "Hiro 保存状态条目格式错误：$key" }
                put(key, encodedValues.map(::decodeValue))
            }
        }
    }

    private fun canBeSaved(value: Any?, visited: IdentityHashMap<Any, Boolean>): Boolean {
        if (value == null) return true
        if (value is Function<*> && value is Serializable) return false
        if (value is Binder || value is Size || value is SizeF || value is Parcelable) return true

        if (value is SnapshotMutableState<*>) {
            return if (value.policyCodeOrNull() == null || visited.put(value, true) != null) false
            else try {
                canBeSaved(value.value, visited)
            } finally {
                visited.remove(value)
            }
        }

        if (value is List<*> || value is Map<*, *> || value is SparseArray<*> || value is Array<*>) {
            return if (visited.put(value, true) != null) false
            else try {
                when (value) {
                    is List<*> -> value.all { canBeSaved(it, visited) }
                    is Map<*, *> -> value.all { (key, item) -> canBeSaved(key, visited) && canBeSaved(item, visited) }
                    is SparseArray<*> -> (0 until value.size).all { canBeSaved(value.valueAt(it), visited) }
                    is Array<*> -> value.all { canBeSaved(it, visited) }
                    else -> false
                }
            } finally {
                visited.remove(value)
            }
        }

        if (value is Serializable) return true
        if (configuration.codecFor(value::class) != null) return true
        return kotlinSerialization.canSerialize(value)
    }

    private fun encodeValue(value: Any?): Bundle {
        require(canBeSaved(value)) { "Hiro 无法将状态写入 Android SavedState：$value" }

        return Bundle().apply {
            when (value) {
                null -> putInt(TYPE, TYPE_NULL)

                is SnapshotMutableState<*> -> {
                    putInt(TYPE, TYPE_STATE)
                    putInt(POLICY, checkNotNull(value.policyCodeOrNull()))
                    putInt(STATE_KIND, value.stateKind())
                    putBundle(VALUE, encodeValue(value.value))
                }

                is List<*> -> {
                    putInt(TYPE, TYPE_LIST)
                    putParcelableArrayList(VALUES, ArrayList(value.map(::encodeValue)))
                }

                is Map<*, *> -> {
                    putInt(TYPE, TYPE_MAP)
                    putParcelableArrayList(VALUES, ArrayList(value.map { (key, item) ->
                        Bundle().apply {
                            putBundle(KEY, encodeValue(key))
                            putBundle(VALUE, encodeValue(item))
                        }
                    }))
                }

                is SparseArray<*> -> {
                    putInt(TYPE, TYPE_SPARSE_ARRAY)
                    putParcelableArrayList(VALUES, ArrayList((0 until value.size()).map { index ->
                        Bundle().apply {
                            putInt(KEY, value.keyAt(index))
                            putBundle(VALUE, encodeValue(value.valueAt(index)))
                        }
                    }))
                }

                is Array<*> -> {
                    putInt(TYPE, TYPE_ARRAY)
                    putString(COMPONENT_TYPE, value.javaClass.componentType.name)
                    putParcelableArrayList(VALUES, ArrayList(value.map(::encodeValue)))
                }

                is Size -> {
                    putInt(TYPE, TYPE_SIZE)
                    putSize(VALUE, value)
                }

                is SizeF -> {
                    putInt(TYPE, TYPE_SIZE_F)
                    putSizeF(VALUE, value)
                }

                is Binder -> {
                    putInt(TYPE, TYPE_BINDER)
                    putBinder(VALUE, value)
                }

                is Parcelable -> {
                    putInt(TYPE, TYPE_PARCELABLE)
                    putParcelable(VALUE, value)
                }

                is Serializable -> {
                    putInt(TYPE, TYPE_SERIALIZABLE)
                    putSerializable(VALUE, value)
                }

                else -> encodeExtendedValue(value)
            }
        }
    }

    private fun decodeValue(encoded: Bundle): Any? {
        encoded.classLoader = classLoader

        return when (val type = encoded.getInt(TYPE, -1)) {
            TYPE_NULL -> null

            TYPE_STATE -> encoded.decodeMutableState()

            TYPE_LIST -> ArrayList(checkNotNull(encoded.getParcelableArrayList<Bundle>(VALUES)).map(::decodeValue))

            TYPE_MAP -> LinkedHashMap<Any?, Any?>().apply {
                checkNotNull(encoded.getParcelableArrayList<Bundle>(VALUES)).forEach { entry ->
                    put(decodeValue(checkNotNull(entry.getBundle(KEY))), decodeValue(checkNotNull(entry.getBundle(VALUE))))
                }
            }

            TYPE_SPARSE_ARRAY -> SparseArray<Any?>().apply {
                checkNotNull(encoded.getParcelableArrayList<Bundle>(VALUES)).forEach { entry ->
                    put(entry.getInt(KEY), decodeValue(checkNotNull(entry.getBundle(VALUE))))
                }
            }

            TYPE_ARRAY -> {
                val componentType = Class.forName(checkNotNull(encoded.getString(COMPONENT_TYPE)), false, classLoader)
                val values = checkNotNull(encoded.getParcelableArrayList<Bundle>(VALUES)).map(::decodeValue)
                java.lang.reflect.Array.newInstance(componentType, values.size).also { array ->
                    values.forEachIndexed { index, value -> java.lang.reflect.Array.set(array, index, value) }
                }
            }

            TYPE_SIZE -> encoded.getSize(VALUE)

            TYPE_SIZE_F -> encoded.getSizeF(VALUE)

            TYPE_BINDER -> encoded.getBinder(VALUE)

            TYPE_PARCELABLE -> encoded.getParcelable(VALUE)

            TYPE_SERIALIZABLE -> encoded.getSerializable(VALUE)

            TYPE_CUSTOM -> encoded.decodeCustomValue()

            TYPE_KOTLIN_SERIALIZED -> kotlinSerialization.deserialize(
                className = checkNotNull(encoded.getString(CLASS_NAME)) { "Hiro Kotlin 序列化状态缺少类名" },
                state = checkNotNull(encoded.getBundle(VALUE)) { "Hiro Kotlin 序列化状态缺少负载" },
            )

            else -> error("Hiro 保存状态含有未知类型标记：$type")
        }
    }

    private fun Bundle.encodeExtendedValue(value: Any) {
        val customCodec = configuration.codecFor(value::class)
        if (customCodec != null) {
            putInt(TYPE, TYPE_CUSTOM)
            putString(TYPE_ID, customCodec.typeId)
            putBundle(VALUE, customCodec.serializeAny(value))
            return
        }

        check(kotlinSerialization.canSerialize(value)) { "Hiro 保存状态编码器遗漏了已允许类型：${value::class.java.name}" }
        putInt(TYPE, TYPE_KOTLIN_SERIALIZED)
        putString(CLASS_NAME, value.javaClass.name)
        putBundle(VALUE, kotlinSerialization.serialize(value))
    }

    private fun Bundle.decodeCustomValue(): Any {
        val typeId = checkNotNull(getString(TYPE_ID)) { "Hiro 自定义 SavableState 缺少 typeId" }
        val codec = checkNotNull(configuration.codecFor(typeId)) { "恢复 Hiro 状态时没有注册 typeId 为 $typeId 的 Codec" }
        val state = checkNotNull(getBundle(VALUE)) { "Hiro 自定义 SavableState 缺少负载：$typeId" }
        state.classLoader = classLoader
        return codec.deserializeAny(state)
    }

    private fun SnapshotMutableState<*>.policyCodeOrNull() = when {
        policy === neverEqualPolicy<Any?>() -> POLICY_NEVER_EQUAL
        policy === structuralEqualityPolicy<Any?>() -> POLICY_STRUCTURAL_EQUALITY
        policy === referentialEqualityPolicy<Any?>() -> POLICY_REFERENTIAL_EQUALITY
        else -> null
    }

    private fun SnapshotMutableState<*>.stateKind() = when (this) {
        is MutableIntState -> STATE_KIND_INT
        is MutableLongState -> STATE_KIND_LONG
        is MutableFloatState -> STATE_KIND_FLOAT
        is MutableDoubleState -> STATE_KIND_DOUBLE
        else -> STATE_KIND_OBJECT
    }

    private fun Bundle.decodeMutableState(): Any {
        val value = decodeValue(checkNotNull(getBundle(VALUE)))

        return when (val stateKind = getInt(STATE_KIND, STATE_KIND_OBJECT)) {
            STATE_KIND_OBJECT -> mutableStateOf(value, getMutationPolicy())
            STATE_KIND_INT -> mutableIntStateOf(value as Int)
            STATE_KIND_LONG -> mutableLongStateOf(value as Long)
            STATE_KIND_FLOAT -> mutableFloatStateOf(value as Float)
            STATE_KIND_DOUBLE -> mutableDoubleStateOf(value as Double)
            else -> error("Hiro 保存状态含有未知 MutableState 类型：$stateKind")
        }
    }

    private fun Bundle.getMutationPolicy(): SnapshotMutationPolicy<Any?> = when (val policy = getInt(POLICY, -1)) {
        POLICY_NEVER_EQUAL -> neverEqualPolicy()
        POLICY_STRUCTURAL_EQUALITY -> structuralEqualityPolicy()
        POLICY_REFERENTIAL_EQUALITY -> referentialEqualityPolicy()
        else -> error("Hiro 保存状态含有未知 MutableState 策略：$policy")
    }

    @Suppress("UNCHECKED_CAST")
    private fun HiroSavableStateCodec<*>.serializeAny(value: Any) = (this as HiroSavableStateCodec<Any>).serialize(value)

    @Suppress("UNCHECKED_CAST")
    private fun HiroSavableStateCodec<*>.deserializeAny(state: Bundle) = (this as HiroSavableStateCodec<Any>).deserialize(state)

    private companion object {
        const val TYPE = "t"
        const val VALUE = "v"
        const val VALUES = "vs"
        const val KEY = "k"
        const val POLICY = "p"
        const val STATE_KIND = "s"
        const val COMPONENT_TYPE = "c"
        const val TYPE_ID = "i"
        const val CLASS_NAME = "n"

        const val TYPE_NULL = 0
        const val TYPE_STATE = 1
        const val TYPE_LIST = 2
        const val TYPE_MAP = 3
        const val TYPE_SPARSE_ARRAY = 4
        const val TYPE_ARRAY = 5
        const val TYPE_PARCELABLE = 6
        const val TYPE_SERIALIZABLE = 7
        const val TYPE_BINDER = 8
        const val TYPE_SIZE = 9
        const val TYPE_SIZE_F = 10
        const val TYPE_CUSTOM = 11
        const val TYPE_KOTLIN_SERIALIZED = 12

        const val POLICY_NEVER_EQUAL = 0
        const val POLICY_STRUCTURAL_EQUALITY = 1
        const val POLICY_REFERENTIAL_EQUALITY = 2

        const val STATE_KIND_OBJECT = 0
        const val STATE_KIND_INT = 1
        const val STATE_KIND_LONG = 2
        const val STATE_KIND_FLOAT = 3
        const val STATE_KIND_DOUBLE = 4
    }
}
