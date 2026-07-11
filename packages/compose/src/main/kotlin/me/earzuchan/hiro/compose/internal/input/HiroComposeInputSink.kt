@file:OptIn(InternalComposeUiApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package me.earzuchan.hiro.compose.internal.input

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.HistoricalChange
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.scene.ComposeScenePointer

@OptIn(InternalComposeUiApi::class)
internal interface HiroComposeInputSink {
    fun sendPointerEvent(event: HiroComposePointerEvent): Boolean

    fun cancelPointerInput()
}

@OptIn(InternalComposeUiApi::class)
internal data class HiroComposePointerEvent(val type: PointerEventType, val pointers: List<ComposeScenePointer>, val buttons: PointerButtons, val keyboardModifiers: PointerKeyboardModifiers, val scrollDelta: Offset = Offset.Zero, val timeMillis: Long, val nativeEvent: Any?, val changedButton: PointerButton? = null, val scaleGestureFactor: Float = 1f, val panGestureOffset: Offset = Offset.Zero)

// 伟大的前置
internal fun HiroComposePointerEvent.coalesceMoveWith(newer: HiroComposePointerEvent): HiroComposePointerEvent? {
    if (type != PointerEventType.Move || newer.type != PointerEventType.Move) return null
    if (newer.timeMillis < timeMillis) return null
    if (buttons != newer.buttons || keyboardModifiers != newer.keyboardModifiers) return null
    if (scrollDelta != Offset.Zero || newer.scrollDelta != Offset.Zero) return null
    if (nativeEvent != null || newer.nativeEvent != null || changedButton != null || newer.changedButton != null) return null
    if (scaleGestureFactor != 1f || newer.scaleGestureFactor != 1f) return null
    if (panGestureOffset != Offset.Zero || newer.panGestureOffset != Offset.Zero) return null
    if (pointers.size != newer.pointers.size) return null

    val mergedPointers = ArrayList<ComposeScenePointer>(pointers.size)
    for (index in pointers.indices) {
        val olderPointer = pointers[index]
        val newerPointer = newer.pointers[index]
        if (olderPointer.id != newerPointer.id || olderPointer.pressed != newerPointer.pressed || olderPointer.type != newerPointer.type) return null

        mergedPointers += ComposeScenePointer(
            id = newerPointer.id,
            position = newerPointer.position,
            pressed = newerPointer.pressed,
            type = newerPointer.type,
            pressure = newerPointer.pressure,
            historical = mergeHistoricalChanges(olderPointer, newerPointer, timeMillis, newer.timeMillis),
        )
    }

    return newer.copy(pointers = mergedPointers)
}

// 混合历史变更以提高性能
private fun mergeHistoricalChanges(older: ComposeScenePointer, newer: ComposeScenePointer, olderTimeMillis: Long, newerTimeMillis: Long): List<HistoricalChange> {
    val earliestTimeMillis = newerTimeMillis - VELOCITY_HISTORY_MILLIS
    val candidates = ArrayList<HistoricalChange>(older.historical.size + newer.historical.size + 1)
    candidates += older.historical
    if (olderTimeMillis < newerTimeMillis) {
        candidates += HistoricalChange(
            uptimeMillis = olderTimeMillis,
            position = older.position,
            scaleFactor = 1f,
            panOffset = Offset.Zero,
            originalEventPosition = older.position,
        )
    }
    candidates += newer.historical

    val sorted = candidates
        .asSequence()
        .filter { it.uptimeMillis >= earliestTimeMillis && it.uptimeMillis < newerTimeMillis }
        .sortedBy(HistoricalChange::uptimeMillis)
        .toList()

    if (sorted.size < 2) return sorted

    val compacted = ArrayList<HistoricalChange>(sorted.size)
    for (change in sorted) {
        if (compacted.lastOrNull()?.uptimeMillis == change.uptimeMillis) compacted[compacted.lastIndex] = change
        else compacted += change
    }
    return compacted
}

private const val VELOCITY_HISTORY_MILLIS = 100L