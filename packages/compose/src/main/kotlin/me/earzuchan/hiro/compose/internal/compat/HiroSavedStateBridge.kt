package me.earzuchan.hiro.compose.internal.compat

import androidx.savedstate.SavedState
import androidx.savedstate.savedState

/**
 * 桌面版 [SavedState] 的安卓桥接，仅供 Hiro Gradle 插件改写后的第三方字节码调用
 *
 * 桌面版 SavedState 是包着 Map 的真类，安卓版则是 Bundle 的别名。第三方桌面变体内联了 `savedState {}` 等构建器后，
 * 字节码里会直接出现 `new SavedState(map)` 与 `state.getMap()`：HGP 把前者改写为构造本类再取 [toSavedState]，后者改写为 [readMap]
 *
 * 构造器形态须与桌面版一一对应（含默认参数生成的合成构造器与无参构造器），签名变动会让改写后的字节码找不到方法
 */
class HiroSavedStateBridge(private val map: MutableMap<String, Any?> = mutableMapOf()) {
    /** 按桌面版构造时给定的 Map 产出安卓 [SavedState] */
    fun toSavedState(): SavedState = savedState(map)

    companion object {
        /** 对应桌面版 `SavedState.getMap()`：返回内容的拷贝，写入不会回流到原状态 */
        @JvmStatic
        @Suppress("DEPRECATION")
        fun readMap(state: SavedState): MutableMap<String, Any?> = state.keySet().associateWithTo(LinkedHashMap()) { state.get(it) }
    }
}
