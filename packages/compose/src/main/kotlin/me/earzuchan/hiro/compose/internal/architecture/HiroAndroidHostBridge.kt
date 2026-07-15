package me.earzuchan.hiro.compose.internal.architecture

import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import me.earzuchan.hiro.compose.internal.util.checkMainThreadForHiroCompose

internal class HiroAndroidHostBridge(private val savedStateTransport: HiroSavedStateTransport, private val savedStateKey: String?, private val onLifecycleChanged: () -> Unit, private val onNavigationBack: () -> Boolean) : AutoCloseable {
    private var view: View? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private var savedStateRegistryOwner: SavedStateRegistryOwner? = null
    private var savedStateProviderKey: String? = null

    private val lifecycleObserver = LifecycleEventObserver { _, _ -> onLifecycleChanged() }

    private val navigationBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (!onNavigationBack()) isEnabled = false
        }
    }

    val lifecycleState: Lifecycle.State
        get() = lifecycleOwner?.lifecycle?.currentState ?: Lifecycle.State.RESUMED

    companion object {
        private const val TAG = "HiroAndroidHostBridge"
        private const val SAVED_STATE_KEY_PREFIX = "me.earzuchan.hiro.compose.HiroComposeView"
    }

    fun attach(view: View) {
        checkMainThreadForHiroCompose()
        check(this.view == null) { "Hiro Android 宿主桥已经挂载" }

        this.view = view
        lifecycleOwner = view.findViewTreeLifecycleOwner()?.also { it.lifecycle.addObserver(lifecycleObserver) }

        val navigationBackOwner = view.findViewTreeOnBackPressedDispatcherOwner()
        if (navigationBackOwner != null) navigationBackOwner.onBackPressedDispatcher.addCallback(navigationBackCallback)
        else Log.w(TAG, "宿主没有 OnBackPressedDispatcherOwner，Hiro Compose 无法接收安卓系统返回事件")

        val savedStateOwner = view.findViewTreeSavedStateRegistryOwner()
        val providerKey = savedStateKey?.let { "$SAVED_STATE_KEY_PREFIX:$it" }
        val restoredState = if (savedStateOwner != null && providerKey != null) savedStateOwner.savedStateRegistry.consumeRestoredStateForKey(providerKey) else null
        savedStateTransport.acceptRestoredState(restoredState)

        if (savedStateOwner != null && providerKey != null) {
            check(savedStateOwner.savedStateRegistry.getSavedStateProvider(providerKey) == null) { "HiroComposeView 的 SavedState key 冲突：$providerKey" }
            savedStateOwner.savedStateRegistry.registerSavedStateProvider(providerKey, savedStateTransport::savedStateForAndroid)
            savedStateRegistryOwner = savedStateOwner
            savedStateProviderKey = providerKey
        } else if (savedStateOwner == null) Log.w(TAG, "宿主没有 SavedStateRegistryOwner，本次 Hiro Compose View 只能保存内存状态")
        else Log.w(TAG, "HiroComposeView 没有稳定 SavedState key，本次只能保存内存状态；请设置稳定 View ID 或显式 key")

        if (lifecycleOwner == null) Log.w(TAG, "宿主没有 LifecycleOwner，Hiro Compose 将仅使用 View 可见性驱动生命周期")
        if (lifecycleOwner == null || navigationBackOwner == null || savedStateOwner == null) {
            Log.w(TAG, "宿主没有提供完整的 AndroidX ViewTree Owner；通常建议从 ComponentActivity 调用 setHiroComposeContent，手动宿主则应自行安装对应 Owner")
        }
        onLifecycleChanged()
    }

    fun updateNavigationBackHandling(enabled: Boolean) {
        checkMainThreadForHiroCompose()

        navigationBackCallback.isEnabled = enabled && view != null
    }

    override fun close() {
        checkMainThreadForHiroCompose()

        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        navigationBackCallback.isEnabled = false
        navigationBackCallback.remove()
        val owner = savedStateRegistryOwner
        val key = savedStateProviderKey
        if (owner != null && key != null) owner.savedStateRegistry.unregisterSavedStateProvider(key)

        view = null
        lifecycleOwner = null
        savedStateRegistryOwner = null
        savedStateProviderKey = null
    }
}
