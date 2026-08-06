@file:OptIn(ExperimentalComposeUiApi::class)
@file:Suppress("DEPRECATION")

package me.earzuchan.hiro.compose.internal.input

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.OffsetMapping
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
    val textLayoutResult: TextLayoutResult?,
    val unclippedTextOffsetInRoot: Offset?,
    val offsetMapping: OffsetMapping,
    val textLayoutToRootTransform: Matrix?,
    val textClippingRectInText: Rect?,
    val textFieldRectInText: Rect?,
)

internal data class HiroImeEditRequest(
    val sessionId: Long,
    val connectionId: Long,
    val sequence: Long,
    val commands: List<EditCommand>,
)

internal data class HiroImeEditAcknowledgement(val connectionId: Long, val sequence: Long)

internal interface HiroImeHost {
    fun requestStartInput()

    fun requestShowKeyboard()

    fun requestHideKeyboard()

    fun startSession(sessionId: Long, revision: Long, imeOptions: ImeOptions, snapshot: HiroImeSnapshot)

    fun updateSession(sessionId: Long, revision: Long, snapshot: HiroImeSnapshot, acknowledgement: HiroImeEditAcknowledgement? = null)

    fun rejectEdit(request: HiroImeEditRequest)

    fun stopSession(sessionId: Long)
}

internal interface HiroImeCommandSink {
    fun enqueueImeEdit(request: HiroImeEditRequest): Boolean

    fun enqueueImeAction(sessionId: Long, action: ImeAction): Boolean

    fun enqueueImeKeyEvent(event: androidx.compose.ui.input.key.KeyEvent): Boolean
}

internal class HiroTextInputCoordinator(private val host: HiroImeHost) : PlatformTextInputService {
    private var activeSession: ActiveSession? = null
    private var legacySession: LegacySession? = null
    private var editInProgress: HiroImeEditRequest? = null

    suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
        val session = ModernSession(NEXT_SESSION_ID.incrementAndGet(), request)
        activate(session)

        try {
            coroutineScope {
                launch {
                    snapshotFlow(session::snapshot).collect { snapshot ->
                        if (activeSession === session) host.updateSession(session.id, session.nextRevision(), snapshot)
                    }
                }

                awaitCancellation()
            }
        } finally {
            deactivate(session)
        }
    }

    fun performEdit(edit: HiroImeEditRequest): Boolean {
        val session = activeSession?.takeIf { it.id == edit.sessionId }
        if (session == null) {
            host.rejectEdit(edit)
            return false
        }
        editInProgress = edit
        try {
            session.onEditCommand(edit.commands)
        } catch (throwable: Throwable) {
            host.rejectEdit(edit)
            throw throwable
        } finally {
            editInProgress = null
        }
        if (activeSession !== session) {
            host.rejectEdit(edit)
            return false
        }
        host.updateSession(
            sessionId = session.id,
            revision = session.nextRevision(),
            snapshot = session.snapshot(),
            acknowledgement = HiroImeEditAcknowledgement(edit.connectionId, edit.sequence),
        )
        return true
    }

    fun performImeAction(sessionId: Long, action: ImeAction): Boolean {
        val session = activeSession?.takeIf { it.id == sessionId } ?: return false
        session.onImeAction(action)
        return true
    }

    override fun startInput(value: TextFieldValue, imeOptions: ImeOptions, onEditCommand: (List<EditCommand>) -> Unit, onImeActionPerformed: (ImeAction) -> Unit) {
        LegacySession(
            id = NEXT_SESSION_ID.incrementAndGet(),
            value = value,
            imeOptions = imeOptions,
            onEditCommandCallback = onEditCommand,
            onImeActionCallback = onImeActionPerformed,
        ).also {
            legacySession = it
            activate(it)
        }
    }

    override fun startInput() = host.requestStartInput()

    override fun stopInput() {
        val session = legacySession ?: return
        legacySession = null
        deactivate(session)
    }

    override fun showSoftwareKeyboard() = host.requestShowKeyboard()

    override fun hideSoftwareKeyboard() = host.requestHideKeyboard()

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) = updateLegacy { it.value = newValue }

    override fun notifyFocusedRect(rect: Rect) = updateLegacy { it.focusedRectInRoot = rect }

    override fun updateTextLayoutResult(textFieldValue: TextFieldValue, offsetMapping: OffsetMapping, textLayoutResult: TextLayoutResult, textFieldToRootTransform: (Matrix) -> Unit, innerTextFieldBounds: Rect, decorationBoxBounds: Rect) = updateLegacy { session ->
        val matrix = Matrix().also(textFieldToRootTransform)
        session.value = textFieldValue
        session.textLayoutResult = textLayoutResult
        session.offsetMapping = offsetMapping
        session.textLayoutToRootTransform = matrix
        session.textClippingRectInText = innerTextFieldBounds
        session.textFieldRectInText = decorationBoxBounds
        session.textFieldRectInRoot = matrix.map(decorationBoxBounds)
        session.textClippingRectInRoot = matrix.map(innerTextFieldBounds)

        val cursorOffset = offsetMapping.originalToTransformed(textFieldValue.selection.max)
        session.focusedRectInRoot = matrix.map(textLayoutResult.getCursorRect(cursorOffset))
        session.unclippedTextOffsetInRoot = session.textClippingRectInRoot?.topLeft?.minus(innerTextFieldBounds.topLeft)
    }

    private fun activate(session: ActiveSession) {
        val previous = activeSession
        if (previous === session) return
        activeSession = session
        if (legacySession !== session) legacySession = null
        previous?.let { host.stopSession(it.id) }
        host.startSession(session.id, session.revision, session.imeOptions, session.snapshot())
    }

    private fun deactivate(session: ActiveSession) {
        if (activeSession !== session) return

        activeSession = null
        if (legacySession === session) legacySession = null
        host.stopSession(session.id)
    }

    private inline fun updateLegacy(update: (LegacySession) -> Unit) {
        val session = legacySession?.takeIf { activeSession === it } ?: return

        update(session)
        if (editInProgress?.sessionId == session.id) return
        host.updateSession(
            sessionId = session.id,
            revision = session.nextRevision(),
            snapshot = session.snapshot(),
        )
    }

    private sealed class ActiveSession(val id: Long, val imeOptions: ImeOptions) {
        var revision: Long = 0L; private set

        fun nextRevision(): Long = ++revision

        abstract fun snapshot(): HiroImeSnapshot

        abstract fun onEditCommand(commands: List<EditCommand>)

        abstract fun onImeAction(action: ImeAction)
    }

    private class ModernSession(id: Long, private val request: PlatformTextInputMethodRequest) : ActiveSession(id, request.imeOptions) {
        override fun snapshot(): HiroImeSnapshot {
            val textOffset = request.unclippedTextOffsetInRoot()
            val textFieldRect = request.textFieldRectInRoot()
            val clippingRect = request.textClippingRectInRoot()
            return HiroImeSnapshot(
                value = request.value(),
                focusedRectInRoot = request.focusedRectInRoot(),
                textFieldRectInRoot = textFieldRect,
                textClippingRectInRoot = clippingRect,
                textLayoutResult = request.textLayoutResult(),
                unclippedTextOffsetInRoot = textOffset,
                offsetMapping = OffsetMapping.Identity,
                textLayoutToRootTransform = textOffset?.let { offset -> Matrix().apply { translate(offset.x, offset.y) } },
                textClippingRectInText = clippingRect?.translate(textOffset?.times(-1f) ?: Offset.Zero),
                textFieldRectInText = textFieldRect?.translate(textOffset?.times(-1f) ?: Offset.Zero),
            )
        }

        override fun onEditCommand(commands: List<EditCommand>) = request.onEditCommand(commands)

        override fun onImeAction(action: ImeAction) {
            request.onImeAction?.invoke(action)
        }
    }

    private class LegacySession(id: Long, var value: TextFieldValue, imeOptions: ImeOptions, private val onEditCommandCallback: (List<EditCommand>) -> Unit, private val onImeActionCallback: (ImeAction) -> Unit) : ActiveSession(id, imeOptions) {
        var focusedRectInRoot: Rect? = null
        var textFieldRectInRoot: Rect? = null
        var textClippingRectInRoot: Rect? = null
        var textLayoutResult: TextLayoutResult? = null
        var unclippedTextOffsetInRoot: Offset? = null
        var offsetMapping: OffsetMapping = OffsetMapping.Identity
        var textLayoutToRootTransform: Matrix? = null
        var textClippingRectInText: Rect? = null
        var textFieldRectInText: Rect? = null

        override fun snapshot() = HiroImeSnapshot(
            value = value,
            focusedRectInRoot = focusedRectInRoot,
            textFieldRectInRoot = textFieldRectInRoot,
            textClippingRectInRoot = textClippingRectInRoot,
            textLayoutResult = textLayoutResult,
            unclippedTextOffsetInRoot = unclippedTextOffsetInRoot,
            offsetMapping = offsetMapping,
            textLayoutToRootTransform = textLayoutToRootTransform,
            textClippingRectInText = textClippingRectInText,
            textFieldRectInText = textFieldRectInText,
        )

        override fun onEditCommand(commands: List<EditCommand>) = onEditCommandCallback(commands)

        override fun onImeAction(action: ImeAction) = onImeActionCallback(action)
    }

    companion object {
        private val NEXT_SESSION_ID = AtomicLong()
    }
}
