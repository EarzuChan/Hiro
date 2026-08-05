@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package me.earzuchan.hiro.compose.internal.input

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType

internal fun AndroidKeyEvent.toHiroComposeKeyEvent(): KeyEvent? {
    val type = when (action) {
        AndroidKeyEvent.ACTION_DOWN -> KeyEventType.KeyDown
        AndroidKeyEvent.ACTION_UP -> KeyEventType.KeyUp
        else -> return null
    }
    return KeyEvent(
        key = Key(keyCode),
        type = type,
        codePoint = unicodeChar,
        isCtrlPressed = isCtrlPressed,
        isMetaPressed = isMetaPressed,
        isAltPressed = isAltPressed,
        isShiftPressed = isShiftPressed,
        nativeEvent = this,
    )
}
