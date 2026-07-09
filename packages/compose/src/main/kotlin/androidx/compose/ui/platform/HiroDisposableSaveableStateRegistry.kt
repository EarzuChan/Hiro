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
import java.io.Serializable

private const val TAG = "HiroDisposableSavableStateRegistry"

// FUCK：龟孙 Jb/谷歌，Savable写成Saveable，我Chovy，写英文给我写好来了啊

internal fun DisposableSaveableStateRegistry(id: String, savedStateRegistryOwner: SavedStateRegistryOwner): DisposableSaveableStateRegistry {
    val key = "SavableStateRegistry:$id"

    val androidxRegistry = savedStateRegistryOwner.savedStateRegistry
    val bundle = androidxRegistry.consumeRestoredStateForKey(key)
    val restored: Map<String, List<Any?>>? = bundle?.toMap()

    val savableStateRegistry = SaveableStateRegistry(restored) { canBeSavedToBundle(it) }

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

private fun canBeSavedToBundle(value: Any): Boolean {
    if (value is SnapshotMutableState<*>) {
        if (value.policy === neverEqualPolicy<Any?>() || value.policy === structuralEqualityPolicy<Any?>() || value.policy === referentialEqualityPolicy<Any?>()) {
            val stateValue = value.value
            return stateValue == null || canBeSavedToBundle(stateValue)
        } else return false
    }

    if (value is Function<*> && value is Serializable) return false

    for (cl in AcceptableClasses) if (cl.isInstance(value)) return true

    return false
}

private val AcceptableClasses = arrayOf(Serializable::class.java, Parcelable::class.java, String::class.java, SparseArray::class.java, Binder::class.java, Size::class.java, SizeF::class.java)

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
        val arrayList = list as? ArrayList<Any?> ?: ArrayList(list)
        bundle.putParcelableArrayList(key, arrayList as ArrayList<Parcelable?>)
    }
    
    return bundle
}
