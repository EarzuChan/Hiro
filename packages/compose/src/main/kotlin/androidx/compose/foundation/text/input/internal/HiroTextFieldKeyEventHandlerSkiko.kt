@file:JvmName("TextFieldKeyEventHandler_skikoKt")
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package androidx.compose.foundation.text.input.internal

import android.view.InputDevice
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.text.KeyCommand
import androidx.compose.foundation.text.KeyModifiers
import androidx.compose.foundation.text.commonKeyMapping
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.internal
import androidx.compose.ui.platform.SoftwareKeyboardController

internal fun createSkikoTextFieldKeyEventHandler() = object : TextFieldKeyEventHandler() {}

internal fun createIOSTextFieldKeyEventHandler() = object : TextFieldKeyEventHandler() {
    override fun onKeyEvent(
        event: KeyEvent,
        textFieldState: TransformedTextFieldState,
        textLayoutState: TextLayoutState,
        textFieldSelectionState: TextFieldSelectionState,
        clipboardKeyCommandsHandler: ClipboardKeyCommandsHandler,
        keyboardController: SoftwareKeyboardController,
        editable: Boolean,
        singleLine: Boolean,
        onSubmit: () -> Boolean,
    ): Boolean = when (commonKeyMapping(KeyModifiers.ShiftMeta).map(event)) {
        KeyCommand.LEFT_CHAR,
        KeyCommand.RIGHT_CHAR,
        KeyCommand.UP,
        KeyCommand.DOWN,
        KeyCommand.SELECT_LEFT_CHAR,
        KeyCommand.SELECT_RIGHT_CHAR,
        KeyCommand.SELECT_UP,
        KeyCommand.SELECT_DOWN -> false
        else -> super.onKeyEvent(
            event,
            textFieldState,
            textLayoutState,
            textFieldSelectionState,
            clipboardKeyCommandsHandler,
            keyboardController,
            editable,
            singleLine,
            onSubmit,
        )
    }
}

internal val KeyEvent.isFromSoftKeyboard: Boolean
    get() = ((internal.nativeEvent as? AndroidKeyEvent)?.flags ?: 0) and AndroidKeyEvent.FLAG_SOFT_KEYBOARD != 0

// TIPS：与 AndroidX 安卓实现一致，来自非虚拟的字母键盘、且不带软键盘标志的按键才算硬件输入
internal val KeyEvent.isFromHardwareSource: Boolean
    get() {
        val nativeEvent = internal.nativeEvent as? AndroidKeyEvent ?: return false
        val device = nativeEvent.device ?: return false
        return !device.isVirtual && device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC && nativeEvent.flags and AndroidKeyEvent.FLAG_SOFT_KEYBOARD == 0
    }
