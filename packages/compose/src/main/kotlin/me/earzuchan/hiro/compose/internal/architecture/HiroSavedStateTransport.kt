package me.earzuchan.hiro.compose.internal.architecture

import android.os.Bundle
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class HiroSavedStateTransport {
    private val restoredStateAccepted = AtomicBoolean(false)
    private val restoredState = AtomicReference<Bundle?>(null)
    private val latestSavedState = AtomicReference(Bundle())

    fun acceptRestoredState(state: Bundle?) {
        if (restoredStateAccepted.compareAndSet(false, true)) restoredState.set(state?.copyForTransport())
    }

    fun snapshotForNewScene() = (restoredState.getAndSet(null) ?: latestSavedState.get()).copyForTransport()

    fun publishSavedState(state: Bundle) = latestSavedState.set(state.copyForTransport())

    fun savedStateForAndroid() = latestSavedState.get().copyForTransport()

    private fun Bundle.copyForTransport() = Bundle(this)
}
