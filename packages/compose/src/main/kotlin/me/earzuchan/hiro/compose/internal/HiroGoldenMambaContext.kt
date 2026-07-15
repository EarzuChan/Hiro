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
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import me.earzuchan.hiro.compose.internal.architecture.HiroArchitectureComponentsOwner
import me.earzuchan.hiro.compose.internal.architecture.HiroSavedStateTransport
import me.earzuchan.hiro.compose.internal.window.HiroMutableWindowInfo
import me.earzuchan.hiro.compose.internal.window.HiroMutableViewConfiguration
import me.earzuchan.hiro.compose.savable.HiroSavableStateConfiguration
import me.earzuchan.hiro.compose.HiroViewConfigurationSnapshot

@OptIn(InternalComposeUiApi::class)
internal class HiroGoldenMambaContext(private val hiroWindowInsets: PlatformWindowInsets, initialEnvironment: HiroComposeEnvironment, requestInputMode: (InputMode) -> Boolean, requestNavigationBackHandling: (Boolean) -> Boolean, savedStateTransport: HiroSavedStateTransport, savableStateConfiguration: HiroSavableStateConfiguration) : PlatformContext.Empty(), AutoCloseable {
    private val hiroArchitectureComponentsOwner = HiroArchitectureComponentsOwner(
        restoredState = savedStateTransport.snapshotForNewScene(),
        publishSavedState = savedStateTransport::publishSavedState,
        requestNavigationBackHandling = requestNavigationBackHandling,
        hiroSavableStateConfiguration = savableStateConfiguration,
    )

    private val hiroInputModeManager = HiroAndroidInputModeManager(requestInputMode)

    private val hiroWindowInfo = HiroMutableWindowInfo()

    private var hiroLocaleList by mutableStateOf(initialEnvironment.localeList)

    private val hiroViewConfiguration = HiroMutableViewConfiguration(initialEnvironment.viewConfiguration)

    override val architectureComponentsOwner: PlatformArchitectureComponentsOwner get() = hiroArchitectureComponentsOwner

    override val inputModeManager: InputModeManager get() = hiroInputModeManager

    override val windowInfo: WindowInfo get() = hiroWindowInfo

    override val windowInsets: PlatformWindowInsets get() = hiroWindowInsets

    override val localeList: LocaleList get() = hiroLocaleList

    override val viewConfiguration: ViewConfiguration get() = hiroViewConfiguration

    fun updateInputMode(inputMode: InputMode) = hiroInputModeManager.update(inputMode)

    fun updateWindowInfo(size: IntSize, density: Density) = hiroWindowInfo.updateContainerSize(size, density)

    fun updateLocaleList(localeList: LocaleList) {
        hiroLocaleList = localeList
    }

    fun updateViewConfiguration(snapshot: HiroViewConfigurationSnapshot) = hiroViewConfiguration.update(snapshot)

    fun moveLifecycleTo(state: Lifecycle.State) = hiroArchitectureComponentsOwner.moveTo(state)

    fun checkpointSavedState() = hiroArchitectureComponentsOwner.checkpointSavedState()

    fun prepareForClose() = hiroArchitectureComponentsOwner.prepareForClose()

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
