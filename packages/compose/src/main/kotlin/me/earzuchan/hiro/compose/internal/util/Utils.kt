package me.earzuchan.hiro.compose.internal.util

import android.os.Looper
import android.view.MotionEvent
import androidx.compose.ui.input.pointer.PointerEventType
import me.earzuchan.hiro.compose.windowinsets.HiroInsetsValues

internal fun checkMainThreadForHiroCompose() = check(Looper.myLooper() == Looper.getMainLooper()) { "Hiro Compose 安卓宿主操作只能在主线程执行" }

internal fun MotionEvent.shouldLogTouchEvent() = when (actionMasked) {
    MotionEvent.ACTION_DOWN,
    MotionEvent.ACTION_UP,
    MotionEvent.ACTION_POINTER_DOWN,
    MotionEvent.ACTION_POINTER_UP,
    MotionEvent.ACTION_CANCEL,
    MotionEvent.ACTION_OUTSIDE -> true
    else -> false
}

internal fun MotionEvent.name() = when (actionMasked) {
    MotionEvent.ACTION_DOWN -> "按下"
    MotionEvent.ACTION_UP -> "抬起"
    MotionEvent.ACTION_POINTER_DOWN -> "多指按下"
    MotionEvent.ACTION_POINTER_UP -> "多指抬起"
    MotionEvent.ACTION_CANCEL -> "取消"
    MotionEvent.ACTION_OUTSIDE -> "越界"
    else -> "其他"
}

internal fun PointerEventType.name() = when (this) {
    PointerEventType.Press -> "按下"
    PointerEventType.Release -> "抬起"
    PointerEventType.Move -> "移动"
    PointerEventType.Enter -> "进入"
    PointerEventType.Exit -> "离开"
    PointerEventType.Scroll -> "滚动"
    else -> "未知"
}

internal fun HiroInsetsValues.logText() = "(${left},${top},${right},${bottom})"
