@file:Suppress("DEPRECATION")

package me.earzuchan.hiro.compose.internal.input

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
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
    private val connectionId: Long,
    initialValue: TextFieldValue,
    private val callbacks: Callbacks,
) : BaseInputConnection(view, false) {
    internal interface Callbacks {
        fun enqueueEdit(request: HiroImeEditRequest): Boolean

        fun sendImeAction(sessionId: Long, action: ImeAction): Boolean

        fun enqueueImeKeyEvent(event: KeyEvent): Boolean

        fun requestCursorUpdates(mode: Int, filter: Int?): Boolean

        fun onConnectionClosed(connection: HiroAndroidInputConnection)
    }

    private val predictionState = HiroImePredictionState(initialValue)
    private var batchDepth = 0
    private val pendingCommands = mutableListOf<EditCommand>()
    private var active = true
    private var extractedTextToken = 0
    private var monitorsExtractedText = false
    private var loggedNullCommitText = false
    private var loggedNullComposingText = false

    private val value: TextFieldValue get() = predictionState.value

    fun updateAuthority(value: TextFieldValue, acknowledgedSequence: Long? = null) {
        if (active) predictionState.reconcile(value, acknowledgedSequence)
    }

    fun matchesConnection(connectionId: Long): Boolean = this.connectionId == connectionId

    fun rejectEdit(sequence: Long) {
        if (active) predictionState.reject(sequence)
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
        return true
    }

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (!active) return false
        if (text == null) {
            if (!loggedNullCommitText) {
                loggedNullCommitText = true
                Log.w(TAG, "IME 调用了 null commitText，已忽略，session=$sessionId，connection=$connectionId")
            }
            return true
        }
        return addCommand(CommitTextCommand(text.toString(), newCursorPosition))
    }

    override fun setComposingRegion(start: Int, end: Int) = addCommand(SetComposingRegionCommand(start, end))

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
        if (!active) return false
        if (text == null) {
            if (!loggedNullComposingText) {
                loggedNullComposingText = true
                Log.w(TAG, "IME 调用了 null setComposingText，已忽略，session=$sessionId，connection=$connectionId")
            }
            return true
        }
        return addCommand(SetComposingTextCommand(text.toString(), newCursorPosition))
    }

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

    override fun sendKeyEvent(event: KeyEvent): Boolean = active && callbacks.enqueueImeKeyEvent(event)

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean =
        active && callbacks.requestCursorUpdates(cursorUpdateMode, null)

    override fun requestCursorUpdates(cursorUpdateMode: Int, cursorUpdateFilter: Int): Boolean =
        active && callbacks.requestCursorUpdates(cursorUpdateMode, cursorUpdateFilter)

    override fun commitCompletion(text: CompletionInfo?): Boolean = false

    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean = false

    override fun clearMetaKeyStates(states: Int): Boolean = false

    override fun reportFullscreenMode(enabled: Boolean): Boolean = false

    override fun performPrivateCommand(action: String?, data: Bundle?): Boolean = false

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
        val applied = try {
            predictionState.apply(commands)
        } catch (throwable: Throwable) {
            Log.w(TAG, "IME 编辑命令无法应用，已忽略，session=$sessionId，connection=$connectionId", throwable)
            return
        }
        val request = HiroImeEditRequest(sessionId, connectionId, applied.sequence, commands)
        if (!callbacks.enqueueEdit(request)) predictionState.reject(applied.sequence)
    }

    private fun sendShortcut(keyCode: Int): Boolean {
        val down = callbacks.enqueueImeKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        val up = callbacks.enqueueImeKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        return down || up
    }

    companion object {
        private const val TAG = "HiroInputConnection"
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
