package me.earzuchan.hiro.compose.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.PlatformArchitectureComponentsOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import me.earzuchan.hiro.compose.interaction.HiroInteractionTuning
import me.earzuchan.hiro.compose.internal.architecture.HiroArchitectureComponentsOwner
import me.earzuchan.hiro.compose.internal.architecture.HiroSavedStateTransport
import me.earzuchan.hiro.compose.internal.interaction.HiroMutableInteractionTuning
import me.earzuchan.hiro.compose.internal.input.HiroImeHost
import me.earzuchan.hiro.compose.internal.input.HiroImeEditRequest
import me.earzuchan.hiro.compose.internal.input.HiroTextInputCoordinator
import me.earzuchan.hiro.compose.internal.focus.HiroAndroidFocusBridge
import me.earzuchan.hiro.compose.internal.window.HiroMutableWindowInfo
import me.earzuchan.hiro.compose.savable.HiroSavableStateConfiguration

@OptIn(InternalComposeUiApi::class)
internal class HiroGoldenMambaContext(private val hiroWindowInsets: PlatformWindowInsets, initialEnvironment: HiroComposeEnvironment, requestInputMode: (InputMode) -> Boolean, requestNavigationBackHandling: (Boolean) -> Boolean, savedStateTransport: HiroSavedStateTransport, savableStateConfiguration: HiroSavableStateConfiguration, private val imeHost: HiroImeHost, private val focusBridge: HiroAndroidFocusBridge, androidPlatformServices: HiroAndroidPlatformServices) : PlatformContext.Empty(), AutoCloseable {
    private val hiroArchitectureComponentsOwner = HiroArchitectureComponentsOwner(
        restoredState = savedStateTransport.snapshotForNewScene(),
        publishSavedState = savedStateTransport::publishSavedState,
        requestNavigationBackHandling = requestNavigationBackHandling,
        hiroSavableStateConfiguration = savableStateConfiguration,
    )

    private val hiroInputModeManager = HiroAndroidInputModeManager(requestInputMode)

    private val hiroWindowInfo = HiroMutableWindowInfo(initialEnvironment.isWindowFocused)

    private var hiroLocaleList by mutableStateOf(initialEnvironment.localeList)

    private val hiroInteractionTuning = HiroMutableInteractionTuning(initialEnvironment.interactionTuning)

    private val hiroTextInputCoordinator = HiroTextInputCoordinator(imeHost)

    override val textToolbar: TextToolbar = androidPlatformServices.createTextToolbar()

    override val architectureComponentsOwner: PlatformArchitectureComponentsOwner get() = hiroArchitectureComponentsOwner

    override val inputModeManager: InputModeManager get() = hiroInputModeManager

    override val windowInfo: WindowInfo get() = hiroWindowInfo

    override val windowInsets: PlatformWindowInsets get() = hiroWindowInsets

    override val localeList: LocaleList get() = hiroLocaleList

    override val viewConfiguration: ViewConfiguration get() = hiroInteractionTuning

    override val parentFocusManager get() = focusBridge

    @Suppress("DEPRECATION")
    override val textInputService: PlatformTextInputService get() = hiroTextInputCoordinator

    override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing = hiroTextInputCoordinator.startInputMethod(request)

    override fun requestFocus(): Boolean = focusBridge.requestViewFocus()

    fun updateInputMode(inputMode: InputMode) = hiroInputModeManager.update(inputMode)

    fun updateWindowInfo(size: IntSize, density: Density) = hiroWindowInfo.updateContainerSize(size, density)

    fun updateWindowFocus(focused: Boolean) = hiroWindowInfo.updateWindowFocus(focused)

    fun updateLocaleList(localeList: LocaleList) {
        hiroLocaleList = localeList
    }

    fun updateInteractionTuning(tuning: HiroInteractionTuning) = hiroInteractionTuning.update(tuning)

    fun moveLifecycleTo(state: Lifecycle.State) = hiroArchitectureComponentsOwner.moveTo(state)

    fun checkpointSavedState() = hiroArchitectureComponentsOwner.checkpointSavedState()

    fun prepareForClose() = hiroArchitectureComponentsOwner.prepareForClose()

    fun dispatchNavigationBack() = hiroArchitectureComponentsOwner.dispatchNavigationBack()

    fun performImeEdit(request: HiroImeEditRequest) = hiroTextInputCoordinator.performEdit(request)

    fun performImeAction(sessionId: Long, action: ImeAction) = hiroTextInputCoordinator.performImeAction(sessionId, action)

    override fun close() {
        textToolbar.hide()
        hiroArchitectureComponentsOwner.close()
    }
}

private class HiroAndroidInputModeManager(private val requestFromHost: (InputMode) -> Boolean) : InputModeManager {
    override var inputMode: InputMode by mutableStateOf(InputMode.Keyboard); private set

    fun update(next: InputMode): Boolean {
        if (inputMode == next) return false

        inputMode = next
        return true
    }

    override fun requestInputMode(inputMode: InputMode): Boolean = requestFromHost(inputMode)
}
