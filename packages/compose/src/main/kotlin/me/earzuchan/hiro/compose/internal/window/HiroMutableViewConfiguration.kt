package me.earzuchan.hiro.compose.internal.window

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.ViewConfiguration
import me.earzuchan.hiro.compose.HiroViewConfigurationSnapshot

internal class HiroMutableViewConfiguration(initial: HiroViewConfigurationSnapshot) : ViewConfiguration {
    private var snapshot by mutableStateOf(initial)

    override val longPressTimeoutMillis get() = snapshot.longPressTimeoutMillis
    override val doubleTapTimeoutMillis get() = snapshot.doubleTapTimeoutMillis
    override val doubleTapMinTimeMillis get() = snapshot.doubleTapMinTimeMillis
    override val touchSlop get() = snapshot.touchSlop
    override val handwritingSlop get() = snapshot.handwritingSlop
    override val minimumTouchTargetSize get() = snapshot.minimumTouchTargetSize
    override val maximumFlingVelocity get() = snapshot.maximumFlingVelocity
    override val minimumFlingVelocity get() = snapshot.minimumFlingVelocity
    override val handwritingGestureLineMargin get() = snapshot.handwritingGestureLineMargin

    fun update(next: HiroViewConfigurationSnapshot): Boolean {
        if (snapshot == next) return false

        snapshot = next
        Snapshot.sendApplyNotifications()
        return true
    }
}
