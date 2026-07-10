package androidx.lifecycle.compose

import android.annotation.SuppressLint
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

private class ComposeLifecycleOwner : LifecycleOwner {
    private val ownerThread = Thread.currentThread()

    @SuppressLint("VisibleForTests")
    private val lifecycleRegistry = LifecycleRegistry.createUnsafe(this)

    override val lifecycle: LifecycleRegistry get() = lifecycleRegistry

    private var parentLifecycleState = Lifecycle.State.INITIALIZED

    var maxLifecycleState = Lifecycle.State.INITIALIZED
        set(value) {
            checkOwnerThread()
            field = value
            updateLifecycleState()
        }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        checkOwnerThread()
        parentLifecycleState = event.targetState
        updateLifecycleState()
    }

    private fun updateLifecycleState() {
        val targetState = minOf(parentLifecycleState, maxLifecycleState)
        if (lifecycleRegistry.currentState == Lifecycle.State.INITIALIZED && targetState == Lifecycle.State.DESTROYED) return
        lifecycleRegistry.currentState = targetState
    }

    private fun checkOwnerThread() = check(Thread.currentThread() === ownerThread) { "ComposeLifecycleOwner 只能由创建它的 Hiro 渲染线程操作" }
}
