package me.earzuchan.hiro.compose.internal

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.PlatformArchitectureComponentsOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

@OptIn(InternalComposeUiApi::class)
internal class HiroAndroidPlatformContext : PlatformContext.Empty() {
    private val hiroArchitectureComponentsOwner = HiroAndroidArchitectureComponentsOwner()

    override val architectureComponentsOwner: PlatformArchitectureComponentsOwner
        get() = hiroArchitectureComponentsOwner

    fun close() {
        hiroArchitectureComponentsOwner.close()
    }
}

@OptIn(InternalComposeUiApi::class)
private class HiroAndroidArchitectureComponentsOwner :
    PlatformArchitectureComponentsOwner,
    LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    NavigationEventDispatcherOwner,
    SavedStateRegistryOwner {

    override val lifecycleOwner: LifecycleOwner
        get() = this

    override val navigationEventDispatcherOwner: NavigationEventDispatcherOwner
        get() = this

    override val viewModelStoreOwner: ViewModelStoreOwner
        get() = this

    override val savedStateRegistryOwner: SavedStateRegistryOwner
        get() = this

    override val lifecycle = LifecycleRegistry(this)
    override val viewModelStore = ViewModelStore()
    override val navigationEventDispatcher = NavigationEventDispatcher()

    private val savedStateController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory = SavedStateViewModelFactory()

    override val defaultViewModelCreationExtras: CreationExtras = MutableCreationExtras().also {
        it[SAVED_STATE_REGISTRY_OWNER_KEY] = this
        it[VIEW_MODEL_STORE_OWNER_KEY] = this
    }

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        enableSavedStateHandles()
        lifecycle.currentState = Lifecycle.State.RESUMED
    }

    fun close() {
        lifecycle.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
