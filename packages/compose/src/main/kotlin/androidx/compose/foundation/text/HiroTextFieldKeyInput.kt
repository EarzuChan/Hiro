@file:JvmName("TextFieldKeyInput_desktopKt")
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package androidx.compose.foundation.text

import androidx.compose.foundation.InternalFoundationApi
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.key.type

@InternalFoundationApi
internal val KeyEvent.isTypedEvent: Boolean
    get() = type == KeyEventType.KeyDown && !Character.isISOControl(utf16CodePoint)
