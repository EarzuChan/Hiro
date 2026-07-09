@file:JvmName("DisposableSaveableStateRegistry_skikoKt")
@file:Suppress("UNCHECKED_CAST")

package androidx.compose.ui.platform

import android.os.Binder
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.savedstate.SavedStateRegistryOwner
import java.io.Serializable as JavaSerializable
import java.util.IdentityHashMap
import androidx.core.util.size

private const val TAG = "HiroDisposableSavableStateRegistry"

// FUCK：龟孙 Jb/谷歌，Savable写成Saveable，我Chovy，写英文给我写好来了啊

internal fun DisposableSaveableStateRegistry(id: String, savedStateRegistryOwner: SavedStateRegistryOwner): DisposableSaveableStateRegistry {
    val key = "SavableStateRegistry:$id"

    val androidxRegistry = savedStateRegistryOwner.savedStateRegistry
    val bundle = androidxRegistry.consumeRestoredStateForKey(key)
    val restored: Map<String, List<Any?>>? = bundle?.toMap()

    val savableStateRegistry = SaveableStateRegistry(restored) { true }

    val registered = if (androidxRegistry.getSavedStateProvider(key) != null) false else try {
        androidxRegistry.registerSavedStateProvider(key) { savableStateRegistry.performSave().toBundle() }
        true
    } catch (_: IllegalArgumentException) {
        Log.d(TAG, "保存状态提供者注册冲突：$key。TODO：多个 Compose 容器使用相同可保存ID名时，我方应提供稳定且可诊断的实例隔离策略")
        false
    }

    return DisposableSaveableStateRegistry(savableStateRegistry) { if (registered) androidxRegistry.unregisterSavedStateProvider(key) }
}

internal class DisposableSaveableStateRegistry(saveableStateRegistry: SaveableStateRegistry, private val onDispose: () -> Unit) : SaveableStateRegistry by saveableStateRegistry {
    fun dispose() = onDispose()
}

private fun canBeSavedToBundle(value: Any?): Boolean = canBeSavedToBundle(value, IdentityHashMap())

private fun canBeSavedToBundle(value: Any?, visited: IdentityHashMap<Any, Boolean>): Boolean {
    if (value == null) return true

    if (visited.put(value, true) == true) return true

    if (value is SnapshotMutableState<*>) {
        if (value.policy === neverEqualPolicy<Any?>() || value.policy === structuralEqualityPolicy<Any?>() || value.policy === referentialEqualityPolicy<Any?>()) {
            val stateValue = value.value
            return canBeSavedToBundle(stateValue, visited)
        } else return false
    }

    if (value is Function<*> && value is JavaSerializable) return false

    @Suppress("DEPRECATION")
    if (value is Bundle) return value.keySet().all { canBeSavedToBundle(value.get(it), visited) }

    if (value is SparseArray<*>) return (0 until value.size).all { canBeSavedToBundle(value.valueAt(it), visited) }

    if (value is Map<*, *>) return value.all { (mapKey, mapValue) -> canBeSavedToBundle(mapKey, visited) && canBeSavedToBundle(mapValue, visited) }

    if (value is Iterable<*>) return value.all { canBeSavedToBundle(it, visited) }

    if (value.javaClass.isArray && !value.javaClass.componentType.isPrimitive) return (value as Array<*>).all { canBeSavedToBundle(it, visited) }

    for (cl in AcceptableClasses) if (cl.isInstance(value)) return true

    return false
}

private val AcceptableClasses = arrayOf(JavaSerializable::class.java, Parcelable::class.java, String::class.java, SparseArray::class.java, Binder::class.java, Size::class.java, SizeF::class.java)

@Suppress("DEPRECATION")
private fun Bundle.toMap(): Map<String, List<Any?>> {
    val map = mutableMapOf<String, List<Any?>>()

    keySet().forEach { key ->
        val list = getParcelableArrayList<Parcelable?>(key) as ArrayList<Any?>
        map[key] = list
    }

    return map
}

private fun Map<String, List<Any?>>.toBundle(): Bundle {
    val bundle = Bundle()

    forEach { (key, list) ->
        if (!list.all { canBeSavedToBundle(it) }) {
            // TODO：搞个 HiroSavableCodec，把 kotlinx.serialization 对象转译为字符串或 SavedState 后持久化
            Log.d(TAG, "跳过无法写入安卓 SavedState 的状态：$key")
            return@forEach
        }

        val arrayList = list as? ArrayList<Any?> ?: ArrayList(list)
        bundle.putParcelableArrayList(key, arrayList as ArrayList<Parcelable?>)
    }
    
    return bundle
}
