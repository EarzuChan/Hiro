package me.earzuchan.hiro.compose.internal

import androidx.compose.ui.input.key.KeyEvent
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal class HiroViewKeyEventRequest(val event: KeyEvent) {
    private sealed interface State {
        data object Waiting : State
        data object Claimed : State
        data class Completed(val handled: Boolean) : State
        data object Cancelled : State
    }

    enum class Result { Handled, Unhandled, Cancelled, ClaimedTimeout }

    private val state = AtomicReference<State>(State.Waiting)
    private val completion = CountDownLatch(1)

    fun claim(): Boolean = state.compareAndSet(State.Waiting, State.Claimed)

    fun complete(handled: Boolean) {
        if (state.compareAndSet(State.Claimed, State.Completed(handled))) completion.countDown()
    }

    fun cancel() {
        if (state.compareAndSet(State.Waiting, State.Cancelled)) completion.countDown()
    }

    fun await(timeoutMillis: Long): Result {
        completion.await(timeoutMillis, TimeUnit.MILLISECONDS)
        while (true) {
            when (val current = state.get()) {
                is State.Completed -> return if (current.handled) Result.Handled else Result.Unhandled
                State.Cancelled -> return Result.Cancelled
                State.Claimed -> return Result.ClaimedTimeout
                State.Waiting -> if (state.compareAndSet(State.Waiting, State.Cancelled)) return Result.Cancelled
            }
        }
    }
}
