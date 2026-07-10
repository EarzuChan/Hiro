package me.earzuchan.hiro.compose.internal.architecture

import androidx.navigationevent.NavigationEventInput

// TIPS：这是最味大的：对COMPOSE NAV的支持
internal class HiroNavigationEventInput(private val requestBackHandling: (Boolean) -> Boolean) : NavigationEventInput() {
    fun backCompleted() = dispatchOnBackCompleted() // 这是接入Nav，告诉Nav触发Back了

    override fun onHasEnabledHandlersChanged(hasEnabledHandlers: Boolean) {
        requestBackHandling(hasEnabledHandlers)
    }

    override fun onRemoved() {
        requestBackHandling(false)
    }
}
