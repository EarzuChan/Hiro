package me.earzuchan.hiro.compose.internal.architecture

import android.os.Looper
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

internal class HiroAndroidHostBridge(private val savedStateTransport: HiroSavedStateTransport, private val onLifecycleChanged: () -> Unit, private val onNavigationBack: () -> Boolean) : AutoCloseable {
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
        checkMainThread()
        check(this.view == null) { "Hiro Android 宿主桥已经挂载" }

        this.view = view
        lifecycleOwner = view.findViewTreeLifecycleOwner()?.also { it.lifecycle.addObserver(lifecycleObserver) }

        val navigationBackOwner = view.findViewTreeOnBackPressedDispatcherOwner()
        if (navigationBackOwner != null) navigationBackOwner.onBackPressedDispatcher.addCallback(navigationBackCallback)
        else Log.w(TAG, "宿主没有 OnBackPressedDispatcherOwner，Hiro Compose 无法接收安卓系统返回事件")

        val savedStateOwner = view.findViewTreeSavedStateRegistryOwner()
        val providerKey = "$SAVED_STATE_KEY_PREFIX:${view.id}"
        val restoredState = savedStateOwner?.savedStateRegistry?.consumeRestoredStateForKey(providerKey)
        savedStateTransport.acceptRestoredState(restoredState)

        if (savedStateOwner != null) {
            check(savedStateOwner.savedStateRegistry.getSavedStateProvider(providerKey) == null) { "HiroComposeView 的 SavedState key 冲突：$providerKey" }
            savedStateOwner.savedStateRegistry.registerSavedStateProvider(providerKey, savedStateTransport::savedStateForAndroid)
            savedStateRegistryOwner = savedStateOwner
            savedStateProviderKey = providerKey
        } else Log.w(TAG, "宿主没有 SavedStateRegistryOwner，本次 Hiro Compose 会话只能保存内存状态")

        if (lifecycleOwner == null) Log.w(TAG, "宿主没有 LifecycleOwner，Hiro Compose 将仅使用 View 可见性驱动生命周期")
        onLifecycleChanged()
    }

    fun updateNavigationBackHandling(enabled: Boolean) {
        checkMainThread()

        navigationBackCallback.isEnabled = enabled && view != null
    }

    override fun close() {
        checkMainThread()

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

    private fun checkMainThread() = check(Looper.myLooper() == Looper.getMainLooper()) { "Hiro Android 宿主桥只能在安卓主线程操作" }
}
