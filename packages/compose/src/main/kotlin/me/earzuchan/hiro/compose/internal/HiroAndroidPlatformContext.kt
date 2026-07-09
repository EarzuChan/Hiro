package me.earzuchan.hiro.compose.internal

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.PlatformArchitectureComponentsOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowInsets
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

@OptIn(InternalComposeUiApi::class)
internal class HiroAndroidPlatformContext(private val hiroWindowInsets: PlatformWindowInsets, requestInputMode: (InputMode) -> Boolean) : PlatformContext.Empty(), AutoCloseable {
    private val hiroArchitectureComponentsOwner = HiroAndroidArchitectureComponentsOwner()
    private val hiroInputModeManager = HiroAndroidInputModeManager(requestInputMode)

    override val architectureComponentsOwner: PlatformArchitectureComponentsOwner get() = hiroArchitectureComponentsOwner
    override val inputModeManager: InputModeManager get() = hiroInputModeManager
    override val windowInsets: PlatformWindowInsets get() = hiroWindowInsets

    fun updateInputMode(inputMode: InputMode) = hiroInputModeManager.update(inputMode)

    fun onHostResume() = hiroArchitectureComponentsOwner.moveTo(Lifecycle.State.RESUMED)

    fun onHostPause() = hiroArchitectureComponentsOwner.moveTo(Lifecycle.State.STARTED)

    override fun close() = hiroArchitectureComponentsOwner.close()
}

private class HiroAndroidInputModeManager(private val requestFromHost: (InputMode) -> Boolean) : InputModeManager {
    override var inputMode: InputMode by mutableStateOf(InputMode.Keyboard); private set

    fun update(next: InputMode) {
        inputMode = next
    }

    override fun requestInputMode(inputMode: InputMode): Boolean = requestFromHost(inputMode)
}

@OptIn(InternalComposeUiApi::class)
private class HiroAndroidArchitectureComponentsOwner : PlatformArchitectureComponentsOwner, LifecycleOwner, ViewModelStoreOwner, HasDefaultViewModelProviderFactory, NavigationEventDispatcherOwner, SavedStateRegistryOwner {
    override val lifecycleOwner get() = this
    override val navigationEventDispatcherOwner get() = this
    override val viewModelStoreOwner get() = this
    override val savedStateRegistryOwner get() = this

    @SuppressLint("VisibleForTests")
    override val lifecycle = LifecycleRegistry.createUnsafe(this)
    override val viewModelStore = ViewModelStore()

    override val navigationEventDispatcher = NavigationEventDispatcher()

    private val savedStateController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    override val defaultViewModelProviderFactory = SavedStateViewModelFactory()

    override val defaultViewModelCreationExtras = MutableCreationExtras().also {
        it[SAVED_STATE_REGISTRY_OWNER_KEY] = this
        it[VIEW_MODEL_STORE_OWNER_KEY] = this
    }

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        enableSavedStateHandles()
        lifecycle.currentState = Lifecycle.State.CREATED
    }

    fun moveTo(state: Lifecycle.State) {
        if (lifecycle.currentState != Lifecycle.State.DESTROYED) lifecycle.currentState = state
    }

    fun close() {
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) return

        lifecycle.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
