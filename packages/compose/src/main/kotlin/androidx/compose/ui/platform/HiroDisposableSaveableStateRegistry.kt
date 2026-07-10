@file:JvmName("DisposableSaveableStateRegistry_skikoKt")
@file:Suppress("UNCHECKED_CAST")

package androidx.compose.ui.platform

import android.util.Log
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import me.earzuchan.hiro.compose.internal.glue.HiroSavableStateBundleCodec

private const val TAG = "HiroDisposableSavableStateRegistry"

// FUCK：龟孙 Jb/谷歌，Savable写成Saveable，我Chovy，写英文给我写好来了啊
internal fun DisposableSaveableStateRegistry(id: String, savedStateRegistryOwner: SavedStateRegistryOwner): DisposableSaveableStateRegistry {
    val key = "SavableStateRegistry:$id"

    val androidxRegistry = savedStateRegistryOwner.savedStateRegistry
    val bundle = androidxRegistry.consumeRestoredStateForKey(key)
    val restored = bundle?.let(HiroSavableStateBundleCodec::decodeRegistry)

    val savableStateRegistry = SaveableStateRegistry(restored, HiroSavableStateBundleCodec::canBeSaved) // CanBeSave这一块

    val registered = if (androidxRegistry.getSavedStateProvider(key) != null) false else try {
        androidxRegistry.registerSavedStateProvider(key) { HiroSavableStateBundleCodec.encodeRegistry(savableStateRegistry.performSave()) }
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
