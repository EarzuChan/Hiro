package me.earzuchan.hiro.compose.internal.architecture

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.PlatformArchitectureComponentsOwner
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import me.earzuchan.hiro.compose.internal.glue.HiroSavableStateConfigurationOwner
import me.earzuchan.hiro.compose.savable.HiroSavableStateConfiguration

@OptIn(InternalComposeUiApi::class)
internal class HiroArchitectureComponentsOwner(restoredState: Bundle?, private val publishSavedState: (Bundle) -> Unit, requestNavigationBackHandling: (Boolean) -> Boolean, override val hiroSavableStateConfiguration: HiroSavableStateConfiguration) : PlatformArchitectureComponentsOwner, LifecycleOwner, ViewModelStoreOwner, HasDefaultViewModelProviderFactory, NavigationEventDispatcherOwner, SavedStateRegistryOwner, HiroSavableStateConfigurationOwner, AutoCloseable {
    private val ownerThread = Thread.currentThread()

    private var closed = false

    override val lifecycleOwner get() = this
    override val navigationEventDispatcherOwner get() = this
    override val viewModelStoreOwner get() = this
    override val savedStateRegistryOwner get() = this

    @SuppressLint("VisibleForTests")
    override val lifecycle = LifecycleRegistry.createUnsafe(this)

    override val viewModelStore = ViewModelStore()
    override val navigationEventDispatcher = NavigationEventDispatcher()
    private val navigationEventInput = HiroNavigationEventInput(requestNavigationBackHandling)

    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override val defaultViewModelProviderFactory = SavedStateViewModelFactory()

    override val defaultViewModelCreationExtras = MutableCreationExtras().also {
        it[SAVED_STATE_REGISTRY_OWNER_KEY] = this
        it[VIEW_MODEL_STORE_OWNER_KEY] = this
    }

    init {
        navigationEventDispatcher.addInput(navigationEventInput)
        savedStateController.performAttach()
        savedStateController.performRestore(restoredState)
        enableSavedStateHandles()
        lifecycle.currentState = Lifecycle.State.CREATED
        checkpointSavedState()
    }

    fun moveTo(state: Lifecycle.State) {
        checkOwnerThread()
        check(state == Lifecycle.State.CREATED || state == Lifecycle.State.STARTED || state == Lifecycle.State.RESUMED) { "Hiro Lifecycle 只能迁移到活动状态：$state" }
        if (closed || lifecycle.currentState == state) return

        lifecycle.currentState = state
        if (state != Lifecycle.State.RESUMED) checkpointSavedState()
    }

    fun checkpointSavedState() {
        checkOwnerThread()
        if (closed) return

        val state = Bundle()
        savedStateController.performSave(state)
        publishSavedState(state)
    }

    fun dispatchNavigationBack() {
        checkOwnerThread()

        if (!closed) navigationEventInput.backCompleted()
    }

    override fun close() {
        checkOwnerThread()
        if (closed) return

        checkpointSavedState()
        closed = true
        navigationEventDispatcher.dispose()
        lifecycle.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }

    private fun checkOwnerThread() = check(Thread.currentThread() === ownerThread) { "Hiro Architecture Components Owner 只能由创建它的渲染线程操作" }
}
