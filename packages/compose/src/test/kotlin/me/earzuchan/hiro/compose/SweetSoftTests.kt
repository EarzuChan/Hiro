@file:OptIn(androidx.compose.ui.InternalComposeUiApi::class)
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package me.earzuchan.hiro.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.scene.ComposeScenePointer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import me.earzuchan.hiro.compose.internal.HiroComposeCommand
import me.earzuchan.hiro.compose.internal.HiroComposeCommandMailbox
import me.earzuchan.hiro.compose.internal.input.HiroComposePointerEvent
import me.earzuchan.hiro.compose.internal.window.HiroMutableWindowInfo
import kotlin.test.*

class MutableWindowInfoTest {
    @Test
    fun updatesPixelAndDpSizesTogether() { // 验证窗口的物理像素与 Dp 尺寸能同步更新且换算准确，并能正确识别和返回变更状态
        val windowInfo = HiroMutableWindowInfo()
        val size = IntSize(1080, 2400)

        assertTrue(windowInfo.updateContainerSize(size, Density(3f)))
        assertEquals(size, windowInfo.containerSize)
        assertEquals(DpSize(360.dp, 800.dp), windowInfo.containerDpSize)
        assertFalse(windowInfo.updateContainerSize(size, Density(3f)))

        assertTrue(windowInfo.updateContainerSize(size, Density(2f)))
        assertEquals(DpSize(540.dp, 1200.dp), windowInfo.containerDpSize)
    }
}

class InputEventMailboxTest {
    @Test
    fun continuousMovesAreMergedWithRecentVelocityHistory() { // 验证连续的移动事件能够正确合并，并保留用于计算速度的最近历史轨迹
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
    fun nonPointerCommandPreservesMoveBoundary() { // 验证非指针指令（如导航返回）会作为边界，阻止前后的移动事件被合并
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
    fun pointerTopologyChangePreventsMoveMerge() { // 验证触摸点 ID 发生变化（多点触控结构改变）时，不会合并移动事件
        val mailbox = HiroComposeCommandMailbox()
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(10L, 10f, pointerId = 1L)))
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(20L, 20f, pointerId = 2L)))

        assertEquals(2, mailbox.takeSnapshot().size)
    }

    @Test
    fun releaseEventPreservesPointerBoundary() { // 验证手指抬起（释放事件）会作为边界，阻止前后的移动事件被合并
        val mailbox = HiroComposeCommandMailbox()
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(10L, 10f)))
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(20L, 20f, type = PointerEventType.Release, pressed = false)))
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(30L, 30f)))

        val commands = mailbox.takeSnapshot()
        assertEquals(3, commands.size)
        assertEquals(PointerEventType.Release, assertIs<HiroComposeCommand.PointerEvent>(commands[1]).event.type)
    }

    @Test
    fun snapshotLeavesLaterCommandsForNextTurn() { // 验证获取快照会消费并清空当前事件，使后续新加入的事件留在下一轮处理
        val mailbox = HiroComposeCommandMailbox()
        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(10L, 10f)))
        assertEquals(1, mailbox.takeSnapshot().size)

        mailbox.add(HiroComposeCommand.PointerEvent(pointerEvent(20L, 20f)))
        assertEquals(1, mailbox.takeSnapshot().size)
    }

    private fun pointerEvent(timeMillis: Long, x: Float, pointerId: Long = 1L, type: PointerEventType = PointerEventType.Move, pressed: Boolean = true) = HiroComposePointerEvent(
        type = type,
        pointers = listOf(ComposeScenePointer(id = PointerId(pointerId), position = Offset(x, 0f), pressed = pressed, type = PointerType.Touch)),
        buttons = PointerButtons(),
        keyboardModifiers = PointerKeyboardModifiers(),
        timeMillis = timeMillis,
        nativeEvent = null,
    )
}
