package me.earzuchan.hiro.compose.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.PlatformArchitectureComponentsOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.lifecycle.Lifecycle
import me.earzuchan.hiro.compose.internal.architecture.HiroArchitectureComponentsOwner
import me.earzuchan.hiro.compose.internal.architecture.HiroSavedStateTransport

@OptIn(InternalComposeUiApi::class)
internal class HiroAndroidPlatformContext(private val hiroWindowInsets: PlatformWindowInsets, requestInputMode: (InputMode) -> Boolean, requestNavigationBackHandling: (Boolean) -> Boolean, savedStateTransport: HiroSavedStateTransport) : PlatformContext.Empty(), AutoCloseable {
    private val hiroArchitectureComponentsOwner = HiroArchitectureComponentsOwner(
        restoredState = savedStateTransport.consumeRestoredState(),
        publishSavedState = savedStateTransport::publishSavedState,
        requestNavigationBackHandling = requestNavigationBackHandling,
    )

    private val hiroInputModeManager = HiroAndroidInputModeManager(requestInputMode)

    override val architectureComponentsOwner: PlatformArchitectureComponentsOwner get() = hiroArchitectureComponentsOwner

    override val inputModeManager: InputModeManager get() = hiroInputModeManager

    override val windowInsets: PlatformWindowInsets get() = hiroWindowInsets

    fun updateInputMode(inputMode: InputMode) = hiroInputModeManager.update(inputMode)

    fun moveLifecycleTo(state: Lifecycle.State) = hiroArchitectureComponentsOwner.moveTo(state)

    fun checkpointSavedState() = hiroArchitectureComponentsOwner.checkpointSavedState()

    fun dispatchNavigationBack() = hiroArchitectureComponentsOwner.dispatchNavigationBack()

    override fun close() = hiroArchitectureComponentsOwner.close()
}

private class HiroAndroidInputModeManager(private val requestFromHost: (InputMode) -> Boolean) : InputModeManager {
    override var inputMode: InputMode by mutableStateOf(InputMode.Keyboard); private set

    fun update(next: InputMode) {
        inputMode = next
    }

    override fun requestInputMode(inputMode: InputMode): Boolean = requestFromHost(inputMode)
}
