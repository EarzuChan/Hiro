package me.earzuchan.hiro.compose.internal.input

import android.content.Context
import android.graphics.Matrix
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.core.view.SoftwareKeyboardControllerCompat
import me.earzuchan.hiro.compose.internal.util.checkMainThreadForHiroCompose

internal class HiroAndroidImeHost(private val view: View) : HiroImeHost, HiroAndroidInputConnection.Callbacks, AutoCloseable {
    private data class Session(val id: Long, val imeOptions: ImeOptions, val snapshot: HiroImeSnapshot)

    private enum class InputCommand { Start, Stop, Show, Hide }

    private val inputMethodManager by lazy(LazyThreadSafetyMode.NONE) { view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
    private val keyboardController by lazy(LazyThreadSafetyMode.NONE) { SoftwareKeyboardControllerCompat(view) }
    private val connections = linkedSetOf<HiroAndroidInputConnection>()
    private val pendingInputCommands = mutableListOf<InputCommand>()
    private val processInputCommandsRunnable = Runnable(::processPendingInputCommands)
    private var commandSink: HiroImeCommandSink? = null
    private var activeSession: Session? = null
    private var inputCommandsScheduled = false
    private var cursorUpdateMode = 0
    private var closed = false

    fun bindCommandSink(commandSink: HiroImeCommandSink) {
        checkMainThreadForHiroCompose()
        check(this.commandSink == null) { "Hiro IME 命令接收器已绑定" }
        this.commandSink = commandSink
    }

    fun isTextEditor(): Boolean = !closed && activeSession != null

    fun createInputConnection(outAttrs: EditorInfo): InputConnection? {
        checkMainThreadForHiroCompose()
        val session = activeSession ?: return null
        outAttrs.configureForHiroIme(session.imeOptions, session.snapshot.value)
        return HiroAndroidInputConnection(
            view = view,
            sessionId = session.id,
            initialValue = session.snapshot.value,
            autoCorrect = session.imeOptions.autoCorrect,
            callbacks = this,
        ).also(connections::add)
    }

    fun onViewFocusChanged(hasFocus: Boolean) {
        checkMainThreadForHiroCompose()
        if (hasFocus && activeSession != null) enqueueInputCommand(InputCommand.Start)
    }

    override fun requestStartInput() = onMain { enqueueInputCommand(InputCommand.Start) }

    override fun requestStopInput() = onMain { enqueueInputCommand(InputCommand.Stop) }

    override fun requestShowKeyboard() = onMain { enqueueInputCommand(InputCommand.Show) }

    override fun requestHideKeyboard() = onMain { enqueueInputCommand(InputCommand.Hide) }

    override fun startSession(sessionId: Long, imeOptions: ImeOptions, snapshot: HiroImeSnapshot) = onMain {
        disposeConnections()
        activeSession = Session(sessionId, imeOptions, snapshot)
        enqueueInputCommand(InputCommand.Start)
    }

    override fun updateSession(sessionId: Long, snapshot: HiroImeSnapshot, origin: HiroImeUpdateOrigin) = onMain {
        val oldSession = activeSession?.takeIf { it.id == sessionId } ?: return@onMain
        activeSession = oldSession.copy(snapshot = snapshot)

        if (oldSession.snapshot.value != snapshot.value) {
            if (origin == HiroImeUpdateOrigin.StateObservation && requiresInputRestart(oldSession.snapshot.value, snapshot.value)) {
                disposeConnections()
                inputMethodManager.restartInput(view)
            } else {
                publishTextState(snapshot)
            }
        }
        if (cursorUpdateMode and InputConnection.CURSOR_UPDATE_MONITOR != 0) publishCursorAnchorInfo()
    }

    override fun stopSession(sessionId: Long) = onMain {
        if (activeSession?.id != sessionId) return@onMain
        activeSession = null
        cursorUpdateMode = 0
        disposeConnections()
        enqueueInputCommand(InputCommand.Stop)
    }

    override fun sendEditCommands(sessionId: Long, commands: List<EditCommand>): Boolean = commandSink?.sendImeEdit(sessionId, commands) == true

    override fun sendImeAction(sessionId: Long, action: ImeAction): Boolean = commandSink?.sendImeAction(sessionId, action) == true

    override fun sendKeyEvent(event: KeyEvent): Boolean = event.toHiroComposeKeyEvent()?.let { commandSink?.sendKeyEvent(it) } == true

    override fun requestCursorUpdates(mode: Int): Boolean {
        cursorUpdateMode = mode
        if (mode and InputConnection.CURSOR_UPDATE_IMMEDIATE != 0) publishCursorAnchorInfo()
        return true
    }

    override fun onConnectionClosed(connection: HiroAndroidInputConnection) {
        connections.remove(connection)
    }

    override fun close() {
        checkMainThreadForHiroCompose()
        if (closed) return
        closed = true
        activeSession = null
        commandSink = null
        pendingInputCommands.clear()
        if (inputCommandsScheduled) view.removeCallbacks(processInputCommandsRunnable)
        inputCommandsScheduled = false
        disposeConnections()
        keyboardController.hide()
        inputMethodManager.restartInput(view)
    }

    private fun enqueueInputCommand(command: InputCommand) {
        if (closed) return
        pendingInputCommands += command
        if (inputCommandsScheduled) return
        inputCommandsScheduled = true
        view.post(processInputCommandsRunnable)
    }

    private fun processPendingInputCommands() {
        inputCommandsScheduled = false
        if (closed || pendingInputCommands.isEmpty()) return

        var startInput: Boolean? = null
        var showKeyboard: Boolean? = null
        pendingInputCommands.forEach { command ->
            when (command) {
                InputCommand.Start -> {
                    startInput = true
                    showKeyboard = true
                }
                InputCommand.Stop -> {
                    startInput = false
                    showKeyboard = false
                }
                InputCommand.Show, InputCommand.Hide -> if (startInput != false) showKeyboard = command == InputCommand.Show
            }
        }
        pendingInputCommands.clear()

        if (startInput == true && activeSession != null) {
            view.requestFocus()
            inputMethodManager.restartInput(view)
        }
        when (showKeyboard) {
            true -> if (activeSession != null) keyboardController.show()
            false -> keyboardController.hide()
            null -> Unit
        }
        if (startInput == false) inputMethodManager.restartInput(view)
    }

    private fun publishCursorAnchorInfo() {
        val session = activeSession ?: return
        val cursor = session.snapshot.focusedRectInRoot ?: return
        val location = IntArray(2).also(view::getLocationOnScreen)
        val matrix = Matrix().apply { setTranslate(location[0].toFloat(), location[1].toFloat()) }
        val info = CursorAnchorInfo.Builder()
            .setMatrix(matrix)
            .setSelectionRange(session.snapshot.value.selection.start, session.snapshot.value.selection.end)
            .setInsertionMarkerLocation(
                cursor.left,
                cursor.top,
                cursor.bottom,
                cursor.bottom,
                CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION,
            )
            .build()
        inputMethodManager.updateCursorAnchorInfo(view, info)
    }

    private fun publishTextState(snapshot: HiroImeSnapshot) {
        connections.forEach { connection ->
            connection.updateValue(snapshot.value)
            if (connection.monitorsExtractedText()) {
                inputMethodManager.updateExtractedText(
                    view,
                    connection.extractedTextToken(),
                    snapshot.value.toExtractedText(),
                )
            }
        }
        val composition = snapshot.value.composition
        inputMethodManager.updateSelection(
            view,
            snapshot.value.selection.min,
            snapshot.value.selection.max,
            composition?.min ?: -1,
            composition?.max ?: -1,
        )
    }

    private fun disposeConnections() {
        connections.forEach(HiroAndroidInputConnection::dispose)
        connections.clear()
    }

    private inline fun onMain(crossinline action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else view.post { if (!closed) action() }
    }

    private fun requiresInputRestart(oldValue: androidx.compose.ui.text.input.TextFieldValue, newValue: androidx.compose.ui.text.input.TextFieldValue): Boolean =
        oldValue.text != newValue.text || oldValue.selection == newValue.selection && oldValue.composition != newValue.composition
}
