package me.earzuchan.hiro.compose.internal.input

import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.TextFieldValue

internal class HiroImePredictionState(initialValue: TextFieldValue) {
    data class AppliedEdit(val sequence: Long, val value: TextFieldValue)

    private data class PendingEdit(val sequence: Long, val commands: List<EditCommand>)

    private val editProcessor = EditProcessor().apply { reset(initialValue, null) }
    private val pendingEdits = mutableListOf<PendingEdit>()
    private var authoritativeValue = initialValue
    private var nextSequence = 0L

    var value: TextFieldValue = initialValue; private set

    fun apply(commands: List<EditCommand>): AppliedEdit {
        val sequence = ++nextSequence
        pendingEdits += PendingEdit(sequence, commands)
        value = try {
            editProcessor.apply(commands)
        } catch (throwable: Throwable) {
            pendingEdits.removeAt(pendingEdits.lastIndex)
            rebase()
            throw throwable
        }
        return AppliedEdit(sequence, value)
    }

    fun reject(sequence: Long) {
        pendingEdits.removeAll { it.sequence == sequence }
        rebase()
    }

    fun reconcile(authoritativeValue: TextFieldValue, acknowledgedSequence: Long?) {
        this.authoritativeValue = authoritativeValue
        if (acknowledgedSequence != null) pendingEdits.removeAll { it.sequence <= acknowledgedSequence }
        rebase()
    }

    private fun rebase() {
        editProcessor.reset(authoritativeValue, null)
        value = authoritativeValue
        pendingEdits.forEach { pending -> value = editProcessor.apply(pending.commands) }
    }
}
