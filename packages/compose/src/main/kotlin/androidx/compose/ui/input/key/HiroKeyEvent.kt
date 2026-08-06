@file:JvmName("KeyEvent_desktopKt")
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package androidx.compose.ui.input.key

import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed

internal data class InternalKeyEvent(
    val key: Key,
    val type: KeyEventType,
    val codePoint: Int,
    val modifiers: PointerKeyboardModifiers,
    val nativeEvent: Any?,
)

internal val KeyEvent.internal: InternalKeyEvent get() = nativeKeyEvent as InternalKeyEvent

val KeyEvent.key: Key get() = internal.key

val KeyEvent.utf16CodePoint: Int get() = internal.codePoint

val KeyEvent.type: KeyEventType get() = internal.type

val KeyEvent.isAltPressed: Boolean get() = internal.modifiers.isAltPressed

val KeyEvent.isCtrlPressed: Boolean get() = internal.modifiers.isCtrlPressed

val KeyEvent.isMetaPressed: Boolean get() = internal.modifiers.isMetaPressed

val KeyEvent.isShiftPressed: Boolean get() = internal.modifiers.isShiftPressed

fun KeyEvent(key: Key, type: KeyEventType, codePoint: Int = 0, isCtrlPressed: Boolean = false, isMetaPressed: Boolean = false, isAltPressed: Boolean = false, isShiftPressed: Boolean = false, nativeEvent: Any? = null) = KeyEvent(
    InternalKeyEvent(
        key = key,
        type = type,
        codePoint = codePoint,
        modifiers = PointerKeyboardModifiers(
            isCtrlPressed = isCtrlPressed,
            isMetaPressed = isMetaPressed,
            isAltPressed = isAltPressed,
            isShiftPressed = isShiftPressed,
        ),
        nativeEvent = nativeEvent,
    )
)

internal fun KeyEvent.copy(key: Key = internal.key, type: KeyEventType = internal.type, codePoint: Int = internal.codePoint, modifiers: PointerKeyboardModifiers = internal.modifiers, nativeEvent: Any? = internal.nativeEvent): KeyEvent = KeyEvent(InternalKeyEvent(key, type, codePoint, modifiers, nativeEvent))
