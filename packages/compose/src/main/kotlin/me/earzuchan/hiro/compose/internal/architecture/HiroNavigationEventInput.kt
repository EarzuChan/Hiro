package me.earzuchan.hiro.compose.internal.architecture

import androidx.navigationevent.NavigationEventInput

internal class HiroNavigationEventInput(private val requestBackHandling: (Boolean) -> Boolean) : NavigationEventInput() {
    fun backCompleted() = dispatchOnBackCompleted()

    override fun onHasEnabledHandlersChanged(hasEnabledHandlers: Boolean) {
        requestBackHandling(hasEnabledHandlers)
    }

    override fun onRemoved() {
        requestBackHandling(false)
    }
}
