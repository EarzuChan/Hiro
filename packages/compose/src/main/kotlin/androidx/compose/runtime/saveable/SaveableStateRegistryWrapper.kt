package androidx.compose.runtime.saveable

import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedState
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.read
import androidx.savedstate.savedState

private const val PROVIDER_KEY = "androidx.savedstate.SavedStateRegistry"

internal class SaveableStateRegistryWrapper(base: SaveableStateRegistry) : SaveableStateRegistry by base, SavedStateRegistryOwner {
    override val lifecycle: LifecycleRegistry
        get() = getOrInitLifecycle()

    private var _lifecycle: LifecycleRegistry? = null
    private var _controller: SavedStateRegistryController? = null

    @Suppress("VisibleForTests")
    private fun getOrInitLifecycle(): LifecycleRegistry = _lifecycle ?: LifecycleRegistry.createUnsafe(this).also { _lifecycle = it }

    private val controller: SavedStateRegistryController
        get() = getOrInitController(savedState = null)

    private fun getOrInitController(savedState: SavedState?): SavedStateRegistryController {
        return _controller ?: SavedStateRegistryController.create(owner = this).also {
            _controller = it
            it.performRestore(savedState)
        }
    }

    override val savedStateRegistry: SavedStateRegistry
        get() = controller.savedStateRegistry

    init {
        val savedState = consumeRestored(key = PROVIDER_KEY) as? SavedState
        if (savedState != null) getOrInitController(savedState)

        registerProvider(key = PROVIDER_KEY) {
            val controller = _controller
            if (controller != null) {
                val result = savedState()
                controller.performSave(outBundle = result)
                if (result.read { isEmpty() }) null else result
            } else {
                null
            }
        }
    }
}
