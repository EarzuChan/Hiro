package me.earzuchan.hiro.compose.internal.input

import android.content.Context
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.core.view.SoftwareKeyboardControllerCompat
import java.util.concurrent.atomic.AtomicLong
import me.earzuchan.hiro.compose.internal.util.checkMainThreadForHiroCompose

internal class HiroAndroidImeHost(private val view: View) : HiroImeHost, HiroAndroidInputConnection.Callbacks, AutoCloseable {
    private data class Session(
        val id: Long,
        val revision: Long,
        val imeOptions: ImeOptions,
        val snapshot: HiroImeSnapshot,
    )

    private sealed interface InputCommand {
        data class Start(val sessionId: Long, val focusGeneration: Long) : InputCommand
        data class Stop(val sessionId: Long) : InputCommand
        data object Show : InputCommand
        data object Hide : InputCommand
    }

    private val inputMethodManager by lazy(LazyThreadSafetyMode.NONE) { view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
    private val keyboardController by lazy(LazyThreadSafetyMode.NONE) { SoftwareKeyboardControllerCompat(view) }
    private val connections = linkedSetOf<HiroAndroidInputConnection>()
    private val pendingInputCommands = mutableListOf<InputCommand>()
    private val processInputCommandsRunnable = Runnable(::processPendingInputCommands)
    private val nextConnectionId = AtomicLong()
    private val focusIntentCounter = AtomicLong()
    private var commandSink: HiroImeCommandSink? = null
    private var activeSession: Session? = null
    private var inputCommandsScheduled = false
    private var cursorAnchorInfoRequest: HiroCursorAnchorInfoRequest? = null
    private var pendingImmediateCursorUpdate = false
    private var loggedCursorAnchorFailure = false
    private var focusGeneration = 0L
    private var minimumValidFocusIntent = 0L
    @Volatile private var closed = false

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
            connectionId = nextConnectionId.incrementAndGet(),
            initialValue = session.snapshot.value,
            callbacks = this,
        ).also(connections::add)
    }

    fun onViewFocusChanged(hasFocus: Boolean) {
        checkMainThreadForHiroCompose()
        focusGeneration++
        if (!hasFocus) minimumValidFocusIntent = focusIntentCounter.incrementAndGet()
        if (hasFocus) activeSession?.let(::enqueueStart)
    }

    override fun requestStartInput() = onMain { activeSession?.let(::enqueueStart) }

    override fun requestShowKeyboard() = onMain { enqueueInputCommand(InputCommand.Show) }

    override fun requestHideKeyboard() = onMain { enqueueInputCommand(InputCommand.Hide) }

    override fun startSession(sessionId: Long, revision: Long, imeOptions: ImeOptions, snapshot: HiroImeSnapshot) = onMain {
        disposeConnections()
        cursorAnchorInfoRequest = null
        pendingImmediateCursorUpdate = false
        activeSession = Session(sessionId, revision, imeOptions, snapshot)
        activeSession?.let(::enqueueStart)
    }

    override fun updateSession(
        sessionId: Long,
        revision: Long,
        snapshot: HiroImeSnapshot,
        acknowledgement: HiroImeEditAcknowledgement?,
    ) = onMain {
        val oldSession = activeSession?.takeIf { it.id == sessionId } ?: return@onMain
        if (revision <= oldSession.revision) return@onMain
        activeSession = oldSession.copy(revision = revision, snapshot = snapshot)

        if (acknowledgement != null) {
            publishTextState(snapshot, acknowledgement)
        } else if (oldSession.snapshot.value != snapshot.value) {
            if (requiresInputRestart(oldSession.snapshot.value, snapshot.value)) {
                disposeConnections()
                inputMethodManager.restartInput(view)
            } else {
                publishTextState(snapshot, null)
            }
        }
        val cursorRequest = cursorAnchorInfoRequest
        if (pendingImmediateCursorUpdate || cursorRequest?.monitor == true) publishCursorAnchorInfo()
    }

    override fun rejectEdit(request: HiroImeEditRequest) = onMain {
        val connection = connections.firstOrNull { it.matchesConnection(request.connectionId) } ?: return@onMain
        connection.rejectEdit(request.sequence)
        connection.dispose()
        connections.remove(connection)
        if (activeSession != null && view.hasFocus()) inputMethodManager.restartInput(view)
    }

    override fun stopSession(sessionId: Long) = onMain {
        if (activeSession?.id != sessionId) return@onMain
        activeSession = null
        cursorAnchorInfoRequest = null
        pendingImmediateCursorUpdate = false
        disposeConnections()
        enqueueInputCommand(InputCommand.Stop(sessionId))
    }

    override fun enqueueEdit(request: HiroImeEditRequest): Boolean = commandSink?.enqueueImeEdit(request) == true

    override fun sendImeAction(sessionId: Long, action: ImeAction): Boolean = commandSink?.enqueueImeAction(sessionId, action) == true

    override fun enqueueImeKeyEvent(event: KeyEvent): Boolean = event.toHiroComposeKeyEvent()?.let { commandSink?.enqueueImeKeyEvent(it) } == true

    override fun requestCursorUpdates(mode: Int, filter: Int?): Boolean {
        val request = resolveCursorAnchorInfoRequest(mode, filter)
        if (request == null) {
            Log.w(TAG, "IME 请求了不受支持的光标锚点更新标志：mode=$mode，filter=$filter")
            return false
        }
        cursorAnchorInfoRequest = request
        pendingImmediateCursorUpdate = mode and InputConnection.CURSOR_UPDATE_IMMEDIATE != 0
        if (pendingImmediateCursorUpdate) publishCursorAnchorInfo()
        return true
    }

    override fun onConnectionClosed(connection: HiroAndroidInputConnection) {
        connections.remove(connection)
    }

    override fun close() {
        checkMainThreadForHiroCompose()
        if (closed) return
        closed = true
        minimumValidFocusIntent = focusIntentCounter.incrementAndGet()
        activeSession = null
        cursorAnchorInfoRequest = null
        pendingImmediateCursorUpdate = false
        commandSink = null
        pendingInputCommands.clear()
        if (inputCommandsScheduled) view.removeCallbacks(processInputCommandsRunnable)
        inputCommandsScheduled = false
        disposeConnections()
        keyboardController.hide()
        inputMethodManager.restartInput(view)
    }

    private fun enqueueStart(session: Session) {
        enqueueInputCommand(InputCommand.Start(session.id, focusGeneration))
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

        if (!view.isFocused && view.rootView.findFocus()?.onCheckIsTextEditor() == true) {
            pendingInputCommands.clear()
            return
        }

        var startInput: InputCommand.Start? = null
        var stopInput: InputCommand.Stop? = null
        var showKeyboard: Boolean? = null
        pendingInputCommands.forEach { command ->
            when (command) {
                is InputCommand.Start -> {
                    startInput = command
                    stopInput = null
                    showKeyboard = true
                }
                is InputCommand.Stop -> {
                    startInput = null
                    stopInput = command
                    showKeyboard = false
                }
                InputCommand.Show, InputCommand.Hide -> if (stopInput == null) showKeyboard = command == InputCommand.Show
            }
        }
        pendingInputCommands.clear()

        val session = activeSession
        if (startInput != null && session?.id == startInput.sessionId && startInput.focusGeneration == focusGeneration && view.hasFocus()) {
            inputMethodManager.restartInput(view)
        }
        when (showKeyboard) {
            true -> if (session != null && view.hasFocus()) keyboardController.show()
            false -> keyboardController.hide()
            null -> Unit
        }
        if (stopInput != null && activeSession?.id != stopInput.sessionId) inputMethodManager.restartInput(view)
    }

    private fun publishCursorAnchorInfo() {
        val session = activeSession ?: return
        val request = cursorAnchorInfoRequest ?: return
        if (!inputMethodManager.isActive(view)) return
        try {
            val info = buildHiroCursorAnchorInfo(view, session.snapshot, request) ?: return
            inputMethodManager.updateCursorAnchorInfo(view, info)
            pendingImmediateCursorUpdate = false
            loggedCursorAnchorFailure = false
        } catch (throwable: Throwable) {
            if (!loggedCursorAnchorFailure) {
                loggedCursorAnchorFailure = true
                Log.w(TAG, "无法构建 IME 光标锚点信息，本次更新已忽略", throwable)
            }
        }
    }

    private fun publishTextState(snapshot: HiroImeSnapshot, acknowledgement: HiroImeEditAcknowledgement?) {
        connections.forEach { connection ->
            val acknowledgedSequence = acknowledgement?.takeIf { connection.matchesConnection(it.connectionId) }?.sequence
            connection.updateAuthority(snapshot.value, acknowledgedSequence)
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

    companion object {
        private const val TAG = "HiroAndroidImeHost"
    }
}
