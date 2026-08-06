@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)

package me.earzuchan.hiro.compose.internal.input

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType

internal fun AndroidKeyEvent.toHiroComposeKeyEvent(): KeyEvent? {
    val snapshot = AndroidKeyEvent(this)
    val type = when (snapshot.action) {
        AndroidKeyEvent.ACTION_DOWN -> KeyEventType.KeyDown
        AndroidKeyEvent.ACTION_UP -> KeyEventType.KeyUp
        else -> return null
    }
    return KeyEvent(
        key = Key(snapshot.keyCode),
        type = type,
        codePoint = snapshot.unicodeChar,
        isCtrlPressed = snapshot.isCtrlPressed,
        isMetaPressed = snapshot.isMetaPressed,
        isAltPressed = snapshot.isAltPressed,
        isShiftPressed = snapshot.isShiftPressed,
        nativeEvent = snapshot,
    )
}
