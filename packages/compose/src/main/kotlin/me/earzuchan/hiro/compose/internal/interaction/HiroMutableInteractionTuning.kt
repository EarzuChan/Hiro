package me.earzuchan.hiro.compose.internal.interaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.ViewConfiguration
import me.earzuchan.hiro.compose.interaction.HiroInteractionTuning

internal class HiroMutableInteractionTuning(initial: HiroInteractionTuning) : ViewConfiguration {
    private var tuning by mutableStateOf(initial)

    override val longPressTimeoutMillis get() = tuning.longPressTimeoutMillis
    override val doubleTapTimeoutMillis get() = tuning.doubleTapTimeoutMillis
    override val doubleTapMinTimeMillis get() = tuning.doubleTapMinTimeMillis
    override val touchSlop get() = tuning.touchSlop
    override val handwritingSlop get() = tuning.handwritingSlop
    override val minimumTouchTargetSize get() = tuning.minimumTouchTargetSize
    override val maximumFlingVelocity get() = tuning.maximumFlingVelocity
    override val minimumFlingVelocity get() = tuning.minimumFlingVelocity
    override val handwritingGestureLineMargin get() = tuning.handwritingGestureLineMargin

    fun update(next: HiroInteractionTuning): Boolean {
        if (tuning == next) return false

        tuning = next
        Snapshot.sendApplyNotifications()
        return true
    }
}
