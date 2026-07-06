package me.earzuchan.hiro.compose.internal.input

import android.view.MotionEvent

internal class HiroAndroidInputRouter(private val sink: HiroComposeInputSink) {
    private val pointerEventAdapter = HiroAndroidPointerEventAdapter()

    // TODO：后续在这里统一挂载虚拟鼠标模式，避免污染普通触摸路径
    fun dispatchTouchEvent(event: MotionEvent) = when (val dispatch = pointerEventAdapter.convertTouchEvent(event)) {
        is HiroAndroidPointerDispatch.Event -> sink.sendPointerEvent(dispatch.event)

        HiroAndroidPointerDispatch.Cancel -> {
            sink.cancelPointerInput()
            true
        }

        null -> false
    }
}

internal sealed class HiroAndroidPointerDispatch {
    data class Event(val event: HiroComposePointerEvent) : HiroAndroidPointerDispatch()

    data object Cancel : HiroAndroidPointerDispatch()
}
