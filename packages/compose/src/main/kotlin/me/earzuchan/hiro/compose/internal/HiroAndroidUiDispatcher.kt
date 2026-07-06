package me.earzuchan.hiro.compose.internal

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

internal object HiroAndroidUiDispatcher : CoroutineDispatcher() {
    private val handler = Handler(Looper.getMainLooper())

    override fun isDispatchNeeded(context: CoroutineContext) = Looper.myLooper() != handler.looper

    override fun dispatch(context: CoroutineContext, block: Runnable) = check(handler.post(block)) { "无法把 Compose Skia Android 任务投递到主线程" }
}
