package androidx.lifecycle.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Composable
@Suppress("StateFlowValueCalledInComposition")
fun <T> StateFlow<T>.collectAsStateWithLifecycle(lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current, minActiveState: Lifecycle.State = Lifecycle.State.STARTED, context: CoroutineContext = EmptyCoroutineContext) = collectAsStateWithLifecycle(value, lifecycleOwner.lifecycle, minActiveState, context)

@Composable
@Suppress("StateFlowValueCalledInComposition")
fun <T> StateFlow<T>.collectAsStateWithLifecycle(lifecycle: Lifecycle, minActiveState: Lifecycle.State = Lifecycle.State.STARTED, context: CoroutineContext = EmptyCoroutineContext) = collectAsStateWithLifecycle(value, lifecycle, minActiveState, context)

@Composable
fun <T> Flow<T>.collectAsStateWithLifecycle(initialValue: T, lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current, minActiveState: Lifecycle.State = Lifecycle.State.STARTED, context: CoroutineContext = EmptyCoroutineContext) = collectAsStateWithLifecycle(initialValue, lifecycleOwner.lifecycle, minActiveState, context)

@Composable
fun <T> Flow<T>.collectAsStateWithLifecycle(initialValue: T, lifecycle: Lifecycle, minActiveState: Lifecycle.State = Lifecycle.State.STARTED, context: CoroutineContext = EmptyCoroutineContext): State<T> = produceState(initialValue, this, lifecycle, minActiveState, context) {
    lifecycle.repeatOnHiroLifecycle(minActiveState) {
        if (context == EmptyCoroutineContext) this@collectAsStateWithLifecycle.collect { this@produceState.value = it } else withContext(context) { this@collectAsStateWithLifecycle.collect { this@produceState.value = it } }
    }
}

private suspend fun Lifecycle.repeatOnHiroLifecycle(state: Lifecycle.State, block: suspend CoroutineScope.() -> Unit) {
    require(state != Lifecycle.State.INITIALIZED) { "repeatOnHiroLifecycle 不允许使用 INITIALIZED 状态" }

    if (currentState == Lifecycle.State.DESTROYED) return

    coroutineScope {
        var launchedJob: Job? = null
        var observer: LifecycleEventObserver? = null

        try {
            suspendCancellableCoroutine { continuation ->
                val startWorkEvent = Lifecycle.Event.upTo(state)
                val cancelWorkEvent = Lifecycle.Event.downFrom(state)
                val mutex = Mutex()

                observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        startWorkEvent -> launchedJob = this@coroutineScope.launch { mutex.withLock { coroutineScope { block() } } }
                        cancelWorkEvent -> launchedJob?.cancel().also { launchedJob = null }
                        Lifecycle.Event.ON_DESTROY -> if (continuation.isActive) continuation.resume(Unit)
                        else -> Unit
                    }
                }

                this@repeatOnHiroLifecycle.addObserver(checkNotNull(observer))
            }
        } finally {
            launchedJob?.cancel()
            observer?.let(::removeObserver)
        }
    }
}
