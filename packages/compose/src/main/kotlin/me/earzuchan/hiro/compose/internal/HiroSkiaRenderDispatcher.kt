package me.earzuchan.hiro.compose.internal

import kotlinx.coroutines.CoroutineDispatcher
import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext

internal class HiroSkiaRenderDispatcher(private val isRenderThread: () -> Boolean, private val enqueueToRenderThread: (Runnable) -> Boolean) : CoroutineDispatcher(), AutoCloseable {
    @Volatile
    private var closed = false

    fun isOnRenderThread(): Boolean = isRenderThread()

    fun tryDispatch(block: Runnable) = if (closed) false
    else if (isOnRenderThread() && HiroRenderDispatcherRegistry.currentDispatcher() === this) {
        runCurrent(block::run)
        true
    } else enqueueToRenderThread(scoped(block))

    fun tryDispatchLater(block: Runnable): Boolean = if (closed) false else enqueueToRenderThread(scoped(block))

    override fun isDispatchNeeded(context: CoroutineContext): Boolean = !isOnRenderThread() || HiroRenderDispatcherRegistry.currentDispatcher() !== this

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (!tryDispatch(block) && !closed) error("无法把 Hiro Compose 任务投递到 Skia 渲染线程")
    }

    override fun close() {
        closed = true
    }

    fun enterCurrent(): HiroSkiaRenderDispatcher? = HiroRenderDispatcherRegistry.enterCurrent(this)

    fun leaveCurrent(previous: HiroSkiaRenderDispatcher?) = HiroRenderDispatcherRegistry.leaveCurrent(previous)

    fun <T> runCurrent(action: () -> T): T = HiroRenderDispatcherRegistry.withCurrent(this, action)

    private fun scoped(block: Runnable) = Runnable { runCurrent(block::run) }
}

internal object HiroRenderDispatcherRegistry {
    private val lock = Any()
    private val dispatchers = mutableListOf<HiroSkiaRenderDispatcher>()
    private val current = ThreadLocal<HiroSkiaRenderDispatcher?>()

    fun register(dispatcher: HiroSkiaRenderDispatcher): AutoCloseable {
        synchronized(lock) {
            check(dispatcher !in dispatchers) { "Hiro 渲染调度器被重复注册" }
            dispatchers += dispatcher
        }

        HiroSnapshotApplyDispatcher.onDispatcherAvailable()
        return AutoCloseable {
            synchronized(lock) { dispatchers.remove(dispatcher) }
            HiroSnapshotApplyDispatcher.onDispatcherAvailable()
        }
    }

    fun currentDispatcher(): HiroSkiaRenderDispatcher? = current.get()

    fun currentRegisteredDispatcher(): HiroSkiaRenderDispatcher? {
        val dispatcher = current.get() ?: return null

        return synchronized(lock) { dispatcher.takeIf(dispatchers::contains) }
    }

    fun snapshot(): List<HiroSkiaRenderDispatcher> = synchronized(lock) { dispatchers.toList() }

    fun enterCurrent(dispatcher: HiroSkiaRenderDispatcher): HiroSkiaRenderDispatcher? {
        val previous = current.get()
        current.set(dispatcher)
        return previous
    }

    fun leaveCurrent(previous: HiroSkiaRenderDispatcher?) {
        current.set(previous)
    }

    fun <T> withCurrent(dispatcher: HiroSkiaRenderDispatcher, action: () -> T): T {
        val previous = enterCurrent(dispatcher)

        return try {
            action()
        } finally {
            leaveCurrent(previous)
        }
    }
}

internal object HiroSnapshotApplyDispatcher : CoroutineDispatcher() {
    private val lock = Any()
    private val pending = ArrayDeque<Runnable>()

    override fun isDispatchNeeded(context: CoroutineContext) = true

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (dispatchToAvailableRenderer(block)) return
        synchronized(lock) { pending.addLast(block) }
        onDispatcherAvailable()
    }

    fun onDispatcherAvailable() {
        while (true) {
            val block = synchronized(lock) { if (pending.isEmpty()) null else pending.removeFirst() } ?: return

            if (!dispatchToAvailableRenderer(block)) {
                synchronized(lock) { pending.addFirst(block) }
                return
            }
        }
    }

    private fun dispatchToAvailableRenderer(block: Runnable): Boolean {
        HiroRenderDispatcherRegistry.currentRegisteredDispatcher()?.let { if (it.tryDispatchLater(block)) return true }

        return HiroRenderDispatcherRegistry.snapshot().any { it.tryDispatchLater(block) }
    }
}
