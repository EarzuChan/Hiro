package me.earzuchan.hiro.compose.savable

import androidx.savedstate.SavedState
import kotlin.reflect.KClass

/** 将一个业务类型与可持久化的 [SavedState] 相互转换 */
interface HiroSavableStateCodec<T : Any> {
    /** 当前 Codec 精确处理的业务类型 */
    val type: KClass<T>

    /** 写入状态快照的稳定类型标识，不应随重构或混淆改变 */
    val typeId: String

    /** 在 Hiro Compose 渲染线程把业务值序列化为状态 */
    fun serialize(value: T): SavedState

    /** 在 Hiro Compose 渲染线程从状态恢复业务值 */
    fun deserialize(state: SavedState): T
}

/** 创建一个精确处理 [T] 的 [HiroSavableStateCodec] */
inline fun <reified T : Any> hiroSavableStateCodec(typeId: String, noinline serialize: (T) -> SavedState, noinline deserialize: (SavedState) -> T): HiroSavableStateCodec<T> {
    require(typeId.isNotBlank()) { "Hiro SavableState Codec 的 typeId 不能为空" }

    return object : HiroSavableStateCodec<T> {
        override val type = T::class
        override val typeId = typeId

        override fun serialize(value: T) = serialize.invoke(value)

        override fun deserialize(state: SavedState) = deserialize.invoke(state)
    }
}
