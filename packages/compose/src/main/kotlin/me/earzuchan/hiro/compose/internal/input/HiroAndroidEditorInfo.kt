package me.earzuchan.hiro.compose.internal.input

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.LocaleList
import androidx.core.view.inputmethod.EditorInfoCompat

internal fun EditorInfo.configureForHiroIme(imeOptions: ImeOptions, value: TextFieldValue) {
    this.imeOptions = imeOptions.imeAction.toEditorAction(imeOptions.singleLine)
    inputType = imeOptions.keyboardType.toAndroidInputType()
    if (imeOptions.keyboardType == KeyboardType.Ascii) {
        this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_FORCE_ASCII
    }

    if (!imeOptions.singleLine && inputType hasFlag InputType.TYPE_CLASS_TEXT) {
        inputType = inputType or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        if (imeOptions.imeAction == ImeAction.Default) this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_NO_ENTER_ACTION
    }

    if (inputType hasFlag InputType.TYPE_CLASS_TEXT) {
        inputType = inputType or when (imeOptions.capitalization) {
            KeyboardCapitalization.Characters -> InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            KeyboardCapitalization.Words -> InputType.TYPE_TEXT_FLAG_CAP_WORDS
            KeyboardCapitalization.Sentences -> InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            else -> 0
        }
        if (imeOptions.autoCorrect) inputType = inputType or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
    }

    initialSelStart = value.selection.start
    initialSelEnd = value.selection.end
    imeOptions.platformImeOptions?.privateImeOptions?.let { privateImeOptions = it }
    hintLocales = when (imeOptions.hintLocales) {
        LocaleList.Empty -> null
        else -> android.os.LocaleList(*imeOptions.hintLocales.map { it.platformLocale }.toTypedArray())
    }
    this.imeOptions = this.imeOptions or EditorInfo.IME_FLAG_NO_FULLSCREEN
    EditorInfoCompat.setInitialSurroundingText(this, value.text)
}

private fun ImeAction.toEditorAction(singleLine: Boolean): Int = when (this) {
    ImeAction.Default -> if (singleLine) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_UNSPECIFIED
    ImeAction.None -> EditorInfo.IME_ACTION_NONE
    ImeAction.Go -> EditorInfo.IME_ACTION_GO
    ImeAction.Next -> EditorInfo.IME_ACTION_NEXT
    ImeAction.Previous -> EditorInfo.IME_ACTION_PREVIOUS
    ImeAction.Search -> EditorInfo.IME_ACTION_SEARCH
    ImeAction.Send -> EditorInfo.IME_ACTION_SEND
    ImeAction.Done -> EditorInfo.IME_ACTION_DONE
    else -> EditorInfo.IME_ACTION_UNSPECIFIED
}

private fun KeyboardType.toAndroidInputType(): Int = when (this) {
    KeyboardType.Text -> InputType.TYPE_CLASS_TEXT
    KeyboardType.Ascii -> InputType.TYPE_CLASS_TEXT
    KeyboardType.Number -> InputType.TYPE_CLASS_NUMBER
    KeyboardType.Phone -> InputType.TYPE_CLASS_PHONE
    KeyboardType.Uri -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
    KeyboardType.Email -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
    KeyboardType.Password -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    KeyboardType.NumberPassword -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
    KeyboardType.Decimal -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    else -> InputType.TYPE_CLASS_TEXT
}

private infix fun Int.hasFlag(flag: Int) = this and flag == flag
