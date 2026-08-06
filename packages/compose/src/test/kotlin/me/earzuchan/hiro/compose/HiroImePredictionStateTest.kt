package me.earzuchan.hiro.compose

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import me.earzuchan.hiro.compose.internal.input.HiroImePredictionState

class HiroImePredictionStateTest {
    @Test
    fun rejectedEditRollsBackToAuthority() {
        val state = HiroImePredictionState(value("甲"))
        val edit = state.apply(listOf(CommitTextCommand("乙", 1)))

        assertEquals("甲乙", state.value.text)
        state.reject(edit.sequence)
        assertEquals(value("甲"), state.value)
    }

    @Test
    fun acknowledgementReplaysLaterPrediction() {
        val state = HiroImePredictionState(value("甲"))
        val first = state.apply(listOf(CommitTextCommand("乙", 1)))
        state.apply(listOf(CommitTextCommand("丙", 1)))

        state.reconcile(value("甲乙"), first.sequence)
        assertEquals(value("甲乙丙"), state.value)
    }

    @Test
    fun rejectingEarlierEditReplaysRemainingPrediction() {
        val state = HiroImePredictionState(value("甲"))
        val first = state.apply(listOf(CommitTextCommand("乙", 1)))
        state.apply(listOf(CommitTextCommand("丙", 1)))

        state.reject(first.sequence)
        assertEquals(value("甲丙"), state.value)
    }

    private fun value(text: String) = TextFieldValue(text, TextRange(text.length))
}
