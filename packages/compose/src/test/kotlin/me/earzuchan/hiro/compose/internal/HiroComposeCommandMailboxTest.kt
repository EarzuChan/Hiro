@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package me.earzuchan.hiro.compose.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.scene.ComposeScenePointer
import me.earzuchan.hiro.compose.internal.input.HiroComposePointerEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HiroComposeCommandMailboxTest {
    @Test
    fun continuousMovesAreMergedWithRecentVelocityHistory() {
        val mailbox = HiroComposeCommandMailbox()
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(0L, 0f)))
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(20L, 20f)))
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(80L, 80f)))
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(140L, 140f)))

        val commands = mailbox.takeSnapshot()
        assertEquals(1, commands.size)

        val event = assertIs<HiroComposeCommand.PointerEvent>(commands.single()).event
        assertEquals(140L, event.timeMillis)
        assertEquals(Offset(140f, 0f), event.pointers.single().position)
        assertEquals(listOf(80L), event.pointers.single().historical.map { it.uptimeMillis })
        assertEquals(Offset(80f, 0f), event.pointers.single().historical.single().originalEventPosition)
    }

    @Test
    fun nonPointerCommandPreservesMoveBoundary() {
        val mailbox = HiroComposeCommandMailbox()
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(10L, 10f)))
        mailbox.add(HiroComposeCommand.NavigationBack)
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(20L, 20f)))

        val commands = mailbox.takeSnapshot()
        assertEquals(3, commands.size)
        assertIs<HiroComposeCommand.PointerEvent>(commands[0])
        assertEquals(HiroComposeCommand.NavigationBack, commands[1])
        assertIs<HiroComposeCommand.PointerEvent>(commands[2])
    }

    @Test
    fun pointerTopologyChangePreventsMoveMerge() {
        val mailbox = HiroComposeCommandMailbox()
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(10L, 10f, pointerId = 1L)))
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(20L, 20f, pointerId = 2L)))

        assertEquals(2, mailbox.takeSnapshot().size)
    }

    @Test
    fun releaseEventPreservesPointerBoundary() {
        val mailbox = HiroComposeCommandMailbox()
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(10L, 10f)))
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(20L, 20f, type = PointerEventType.Release, pressed = false)))
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(30L, 30f)))

        val commands = mailbox.takeSnapshot()
        assertEquals(3, commands.size)
        assertEquals(PointerEventType.Release, assertIs<HiroComposeCommand.PointerEvent>(commands[1]).event.type)
    }

    @Test
    fun snapshotLeavesLaterCommandsForNextTurn() {
        val mailbox = HiroComposeCommandMailbox()
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(10L, 10f)))
        assertEquals(1, mailbox.takeSnapshot().size)

        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(20L, 20f)))
        assertEquals(1, mailbox.takeSnapshot().size)
    }

    private fun pointerEvent(timeMillis: Long, x: Float, pointerId: Long = 1L, type: PointerEventType = PointerEventType.Move, pressed: Boolean = true) = HiroComposePointerEvent(
        type = type,
        pointers = listOf(
            ComposeScenePointer(
                id = PointerId(pointerId),
                position = Offset(x, 0f),
                pressed = pressed,
                type = PointerType.Touch,
            )
        ),
        buttons = PointerButtons(),
        keyboardModifiers = PointerKeyboardModifiers(),
        timeMillis = timeMillis,
        nativeEvent = null,
    )
}
