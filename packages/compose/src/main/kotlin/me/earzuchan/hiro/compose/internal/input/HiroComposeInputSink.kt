package me.earzuchan.hiro.compose.internal.input

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
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
internal data class HiroComposePointerEvent(
    val type: PointerEventType,
    val pointers: List<ComposeScenePointer>,
    val buttons: PointerButtons,
    val keyboardModifiers: PointerKeyboardModifiers,
    val scrollDelta: Offset = Offset.Zero,
    val timeMillis: Long,
    val nativeEvent: Any?,
    val changedButton: PointerButton? = null,
    val scaleGestureFactor: Float = 1f,
    val panGestureOffset: Offset = Offset.Zero,
)
