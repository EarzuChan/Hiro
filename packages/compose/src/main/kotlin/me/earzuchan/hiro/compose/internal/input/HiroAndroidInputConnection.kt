@file:Suppress("DEPRECATION")

package me.earzuchan.hiro.compose.internal.input

import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextInCodePointsCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.FinishComposingTextCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.SetComposingRegionCommand
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.SetSelectionCommand
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import androidx.compose.ui.text.input.getTextAfterSelection
import androidx.compose.ui.text.input.getTextBeforeSelection

internal class HiroAndroidInputConnection(
    view: View,
    private val sessionId: Long,
    initialValue: TextFieldValue,
    private val autoCorrect: Boolean,
    private val callbacks: Callbacks,
) : BaseInputConnection(view, false) {
    internal interface Callbacks {
        fun sendEditCommands(sessionId: Long, commands: List<EditCommand>): Boolean

        fun sendImeAction(sessionId: Long, action: ImeAction): Boolean

        fun sendKeyEvent(event: KeyEvent): Boolean

        fun requestCursorUpdates(mode: Int): Boolean

        fun onConnectionClosed(connection: HiroAndroidInputConnection)
    }

    private var value = initialValue
    private val editProcessor = EditProcessor().apply { reset(initialValue, null) }
    private var batchDepth = 0
    private val pendingCommands = mutableListOf<EditCommand>()
    private var active = true
    private var extractedTextToken = 0
    private var monitorsExtractedText = false

    fun updateValue(value: TextFieldValue) {
        if (active) {
            this.value = value
            editProcessor.reset(value, null)
        }
    }

    fun monitorsExtractedText(): Boolean = monitorsExtractedText

    fun extractedTextToken(): Int = extractedTextToken

    override fun beginBatchEdit(): Boolean {
        if (!active) return false
        batchDepth++
        return true
    }

    override fun endBatchEdit(): Boolean {
        if (!active || batchDepth == 0) return false
        batchDepth--
        flushCommandsIfReady()
        return batchDepth > 0
    }

    override fun commitText(text: CharSequence?, newCursorPosition: Int) = addCommand(CommitTextCommand(text.toString(), newCursorPosition))

    override fun setComposingRegion(start: Int, end: Int) = addCommand(SetComposingRegionCommand(start, end))

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int) = addCommand(SetComposingTextCommand(text.toString(), newCursorPosition))

    override fun finishComposingText() = addCommand(FinishComposingTextCommand())

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int) = addCommand(DeleteSurroundingTextCommand(beforeLength, afterLength))

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int) = addCommand(DeleteSurroundingTextInCodePointsCommand(beforeLength, afterLength))

    override fun setSelection(start: Int, end: Int) = addCommand(SetSelectionCommand(start, end))

    override fun getTextBeforeCursor(maxChars: Int, flags: Int): CharSequence = value.getTextBeforeSelection(maxChars).toString()

    override fun getTextAfterCursor(maxChars: Int, flags: Int): CharSequence = value.getTextAfterSelection(maxChars).toString()

    override fun getSelectedText(flags: Int): CharSequence? = if (value.selection.collapsed) null else value.getSelectedText().toString()

    override fun getCursorCapsMode(reqModes: Int): Int = TextUtils.getCapsMode(value.text, value.selection.min, reqModes)

    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText {
        monitorsExtractedText = flags and GET_EXTRACTED_TEXT_MONITOR != 0
        if (monitorsExtractedText) extractedTextToken = request?.token ?: 0
        return value.toExtractedText()
    }

    override fun performEditorAction(editorAction: Int): Boolean {
        if (!active) return false
        val action = when (editorAction) {
            EditorInfo.IME_ACTION_DONE -> ImeAction.Done
            EditorInfo.IME_ACTION_SEND -> ImeAction.Send
            EditorInfo.IME_ACTION_SEARCH -> ImeAction.Search
            EditorInfo.IME_ACTION_PREVIOUS -> ImeAction.Previous
            EditorInfo.IME_ACTION_NEXT -> ImeAction.Next
            EditorInfo.IME_ACTION_GO -> ImeAction.Go
            EditorInfo.IME_ACTION_NONE -> ImeAction.None
            else -> ImeAction.Default
        }
        return callbacks.sendImeAction(sessionId, action)
    }

    override fun performContextMenuAction(id: Int): Boolean {
        if (!active) return false
        return when (id) {
            android.R.id.selectAll -> addCommand(SetSelectionCommand(0, value.text.length))
            android.R.id.cut -> sendShortcut(KeyEvent.KEYCODE_CUT)
            android.R.id.copy -> sendShortcut(KeyEvent.KEYCODE_COPY)
            android.R.id.paste -> sendShortcut(KeyEvent.KEYCODE_PASTE)
            else -> false
        }
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean = active && callbacks.sendKeyEvent(event)

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean = active && callbacks.requestCursorUpdates(cursorUpdateMode)

    override fun commitCompletion(text: CompletionInfo?): Boolean = false

    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean = active && autoCorrect

    override fun clearMetaKeyStates(states: Int): Boolean = false

    override fun reportFullscreenMode(enabled: Boolean): Boolean = false

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean = active

    override fun closeConnection() {
        if (!active) return
        active = false
        batchDepth = 0
        pendingCommands.clear()
        callbacks.onConnectionClosed(this)
        super.closeConnection()
    }

    fun dispose() {
        if (!active) return
        active = false
        batchDepth = 0
        pendingCommands.clear()
    }

    private fun addCommand(command: EditCommand): Boolean {
        if (!active) return false
        pendingCommands += command
        flushCommandsIfReady()
        return true
    }

    private fun flushCommandsIfReady() {
        if (batchDepth != 0 || pendingCommands.isEmpty()) return
        val commands = pendingCommands.toList()
        pendingCommands.clear()
        value = editProcessor.apply(commands)
        callbacks.sendEditCommands(sessionId, commands)
    }

    private fun sendShortcut(keyCode: Int): Boolean {
        val down = callbacks.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        val up = callbacks.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        return down || up
    }
}

internal fun TextFieldValue.toExtractedText() = ExtractedText().also { extracted ->
    extracted.text = text
    extracted.startOffset = 0
    extracted.partialStartOffset = -1
    extracted.partialEndOffset = text.length
    extracted.selectionStart = selection.min
    extracted.selectionEnd = selection.max
    extracted.flags = if ('\n' !in text) ExtractedText.FLAG_SINGLE_LINE else 0
}
