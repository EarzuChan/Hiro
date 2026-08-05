@file:OptIn(ExperimentalComposeUiApi::class)
@file:Suppress("DEPRECATION")

package me.earzuchan.hiro.compose.internal.input

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal data class HiroImeSnapshot(
    val value: TextFieldValue,
    val focusedRectInRoot: Rect?,
    val textFieldRectInRoot: Rect?,
    val textClippingRectInRoot: Rect?,
)

internal enum class HiroImeUpdateOrigin { StateObservation, ImeEdit }

internal interface HiroImeHost {
    fun requestStartInput()

    fun requestStopInput()

    fun requestShowKeyboard()

    fun requestHideKeyboard()

    fun startSession(sessionId: Long, imeOptions: ImeOptions, snapshot: HiroImeSnapshot)

    fun updateSession(sessionId: Long, snapshot: HiroImeSnapshot, origin: HiroImeUpdateOrigin)

    fun stopSession(sessionId: Long)
}

internal interface HiroImeCommandSink {
    fun sendImeEdit(sessionId: Long, commands: List<EditCommand>): Boolean

    fun sendImeAction(sessionId: Long, action: ImeAction): Boolean

    fun sendKeyEvent(event: androidx.compose.ui.input.key.KeyEvent): Boolean
}

internal class HiroRenderTextInputSession(private val host: HiroImeHost) {
    private var activeSession: ActiveSession? = null

    suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
        val session = ActiveSession(NEXT_SESSION_ID.incrementAndGet(), request)
        check(activeSession == null) { "Hiro IME 渲染会话发生重叠" }
        activeSession = session

        try {
            coroutineScope {
                launch {
                    snapshotFlow(session::snapshot).collect { snapshot ->
                        host.updateSession(session.id, snapshot, HiroImeUpdateOrigin.StateObservation)
                    }
                }
                host.startSession(session.id, request.imeOptions, session.snapshot())
                awaitCancellation()
            }
        } finally {
            if (activeSession === session) activeSession = null
            host.stopSession(session.id)
        }
    }

    fun performEdit(sessionId: Long, commands: List<EditCommand>): Boolean {
        val session = activeSession?.takeIf { it.id == sessionId } ?: return false
        session.request.onEditCommand(commands)
        host.updateSession(session.id, session.snapshot(), HiroImeUpdateOrigin.ImeEdit)
        return true
    }

    fun performImeAction(sessionId: Long, action: ImeAction): Boolean {
        val session = activeSession?.takeIf { it.id == sessionId } ?: return false
        session.request.onImeAction?.invoke(action)
        return true
    }

    private class ActiveSession(val id: Long, val request: PlatformTextInputMethodRequest) {
        fun snapshot() = HiroImeSnapshot(
            value = request.value(),
            focusedRectInRoot = request.focusedRectInRoot(),
            textFieldRectInRoot = request.textFieldRectInRoot(),
            textClippingRectInRoot = request.textClippingRectInRoot(),
        )
    }

    companion object {
        private val NEXT_SESSION_ID = AtomicLong()
    }
}

internal class HiroPlatformTextInputService(private val host: HiroImeHost) : PlatformTextInputService {
    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit,
    ) = host.requestStartInput()

    override fun stopInput() = host.requestStopInput()

    override fun showSoftwareKeyboard() = host.requestShowKeyboard()

    override fun hideSoftwareKeyboard() = host.requestHideKeyboard()

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) = Unit
}
