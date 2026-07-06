package me.earzuchan.hiro.compose.internal

import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.PlatformArchitectureComponentsOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

@OptIn(InternalComposeUiApi::class)
internal class HiroAndroidPlatformContext(private val hiroWindowInsets: PlatformWindowInsets) : PlatformContext.Empty() {
    private val hiroArchitectureComponentsOwner = HiroAndroidArchitectureComponentsOwner()
    private val hiroInputModeManager = HiroAndroidInputModeManager()

    override val architectureComponentsOwner: PlatformArchitectureComponentsOwner get() = hiroArchitectureComponentsOwner

    override val inputModeManager: InputModeManager get() = hiroInputModeManager

    override val windowInsets: PlatformWindowInsets get() = hiroWindowInsets

    fun attachHostView(view: View) = hiroInputModeManager.attachHostView(view)

    fun detachHostView(view: View) = hiroInputModeManager.detachHostView(view)

    fun close() {
        hiroInputModeManager.close()
        hiroArchitectureComponentsOwner.close()
    }
}

private class HiroAndroidInputModeManager : InputModeManager {
    private var hostView: View? = null
    private var hostViewTreeObserver: ViewTreeObserver? = null

    private val touchModeListener = ViewTreeObserver.OnTouchModeChangeListener { isInTouchMode -> inputMode = if (isInTouchMode) InputMode.Touch else InputMode.Keyboard }

    override var inputMode: InputMode by mutableStateOf(InputMode.Keyboard); private set

    fun attachHostView(view: View) {
        if (hostView === view) {
            syncInputModeFromHostView()
            return
        }

        hostView?.let(::detachHostView)

        hostView = view
        inputMode = view.currentInputMode()

        hostViewTreeObserver = view.viewTreeObserver.also { it.addOnTouchModeChangeListener(touchModeListener) }
    }

    fun detachHostView(view: View) {
        if (hostView !== view) return

        hostViewTreeObserver?.takeIf { it.isAlive }?.removeOnTouchModeChangeListener(touchModeListener)
        hostViewTreeObserver = null
        hostView = null
    }

    fun close() = hostView?.let(::detachHostView)

    override fun requestInputMode(inputMode: InputMode): Boolean {
        val view = hostView ?: return this.inputMode == inputMode

        return when (inputMode) {
            InputMode.Touch -> {
                syncInputModeFromHostView()
                view.isInTouchMode
            }

            InputMode.Keyboard -> {
                val changed = if (view.isInTouchMode) view.requestFocusFromTouch() else true
                syncInputModeFromHostView()
                changed
            }

            else -> false
        }
    }

    private fun syncInputModeFromHostView() = hostView?.let { view -> inputMode = view.currentInputMode() }

    private fun View.currentInputMode(): InputMode = if (isInTouchMode) InputMode.Touch else InputMode.Keyboard

    // TODO：未来接入真鼠标、虚拟鼠标和触控板时，需要确认它们对输入模式的语义是否仍跟随安卓触摸模式
}

@OptIn(InternalComposeUiApi::class)
private class HiroAndroidArchitectureComponentsOwner : PlatformArchitectureComponentsOwner, LifecycleOwner, ViewModelStoreOwner, HasDefaultViewModelProviderFactory, NavigationEventDispatcherOwner, SavedStateRegistryOwner {
    override val lifecycleOwner: LifecycleOwner get() = this

    override val navigationEventDispatcherOwner: NavigationEventDispatcherOwner get() = this

    override val viewModelStoreOwner: ViewModelStoreOwner get() = this

    override val savedStateRegistryOwner: SavedStateRegistryOwner get() = this

    override val lifecycle = LifecycleRegistry(this)
    override val viewModelStore = ViewModelStore()
    override val navigationEventDispatcher = NavigationEventDispatcher()

    private val savedStateController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

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
