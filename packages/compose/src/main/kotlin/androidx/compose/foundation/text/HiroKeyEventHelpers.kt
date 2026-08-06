@file:JvmName("KeyEventHelpers_desktopKt")
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package androidx.compose.foundation.text

import androidx.compose.ui.input.key.KeyEvent

internal fun KeyEvent.cancelsTextSelection(): Boolean = false

internal fun showCharacterPalette() = Unit
