package me.earzuchan.hiro.compose.savable

import androidx.savedstate.SavedState
import kotlin.reflect.KClass

/** 以特制规则将一个业务类型与可持久化的 [SavedState] 相互转换 */
interface HiroSavableStateSeDes<T : Any> {
    /** 当前 SeDes 精确处理的业务类型 */
    val type: KClass<T>

    /** 在 Hiro Compose 渲染线程把业务值序列化为状态 */
    fun serialize(value: T): SavedState

    /** 在 Hiro Compose 渲染线程从状态恢复业务值 */
    fun deserialize(state: SavedState): T
}

/** 创建一个以特制规则精确处理 [T] 的 [HiroSavableStateSeDes] */
inline fun <reified T : Any> hiroSavableStateSeDes(noinline serialize: (T) -> SavedState, noinline deserialize: (SavedState) -> T): HiroSavableStateSeDes<T> {
    return object : HiroSavableStateSeDes<T> {
        override val type = T::class

        override fun serialize(value: T) = serialize.invoke(value)

        override fun deserialize(state: SavedState) = deserialize.invoke(state)
    }
}
