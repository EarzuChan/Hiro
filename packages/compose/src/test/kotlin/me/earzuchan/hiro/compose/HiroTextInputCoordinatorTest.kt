package me.earzuchan.hiro.compose

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import me.earzuchan.hiro.compose.internal.input.HiroImeEditAcknowledgement
import me.earzuchan.hiro.compose.internal.input.HiroImeEditRequest
import me.earzuchan.hiro.compose.internal.input.HiroImeHost
import me.earzuchan.hiro.compose.internal.input.HiroImeSnapshot
import me.earzuchan.hiro.compose.internal.input.HiroTextInputCoordinator

class HiroTextInputCoordinatorTest {
    @Test
    fun legacyPredictionIsPublishedOnceWithAcknowledgement() {
        val host = RecordingImeHost()
        lateinit var coordinator: HiroTextInputCoordinator
        coordinator = HiroTextInputCoordinator(host)
        coordinator.startInput(
            value = value("甲"),
            imeOptions = ImeOptions.Default,
            onEditCommand = { coordinator.updateState(null, value("甲乙")) },
            onImeActionPerformed = {},
        )

        val sessionId = assertNotNull(host.startedSessionId)
        assertTrue(
            coordinator.performEdit(
                HiroImeEditRequest(
                    sessionId = sessionId,
                    connectionId = 7L,
                    sequence = 11L,
                    commands = listOf(CommitTextCommand("乙", 1)),
                )
            )
        )

        assertEquals(1, host.updates.size)
        assertEquals(value("甲乙"), host.updates.single().snapshot.value)
        assertEquals(HiroImeEditAcknowledgement(7L, 11L), host.updates.single().acknowledgement)
    }

    private class RecordingImeHost : HiroImeHost {
        data class Update(
            val snapshot: HiroImeSnapshot,
            val acknowledgement: HiroImeEditAcknowledgement?,
        )

        var startedSessionId: Long? = null
        val updates = mutableListOf<Update>()

        override fun requestStartInput() = Unit

        override fun requestShowKeyboard() = Unit

        override fun requestHideKeyboard() = Unit

        override fun requestViewFocus(): Boolean = true

        override fun startSession(
            sessionId: Long,
            revision: Long,
            imeOptions: ImeOptions,
            snapshot: HiroImeSnapshot,
        ) {
            startedSessionId = sessionId
        }

        override fun updateSession(
            sessionId: Long,
            revision: Long,
            snapshot: HiroImeSnapshot,
            acknowledgement: HiroImeEditAcknowledgement?,
        ) {
            updates += Update(snapshot, acknowledgement)
        }

        override fun rejectEdit(request: HiroImeEditRequest) = Unit

        override fun stopSession(sessionId: Long) = Unit
    }

    private fun value(text: String) = TextFieldValue(text, TextRange(text.length))
}
