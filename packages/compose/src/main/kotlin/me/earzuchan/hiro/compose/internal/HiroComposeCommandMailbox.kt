package me.earzuchan.hiro.compose.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.focus.FocusDirection
import androidx.lifecycle.Lifecycle
import me.earzuchan.hiro.compose.internal.input.HiroComposePointerEvent
import me.earzuchan.hiro.compose.internal.input.HiroImeEditRequest
import me.earzuchan.hiro.compose.internal.input.coalesceMoveWith
import java.util.ArrayDeque

internal class HiroComposeCommandMailbox {
    private val lock = Any()
    private val commands = ArrayDeque<HiroComposeCommand>()

    fun add(current: HiroComposeCommand) {
        synchronized(lock) {
            val previous = commands.peekLast()

            if (previous is HiroComposeCommand.PointerEvent && current is HiroComposeCommand.PointerEvent) previous.event.coalesceMoveWith(current.event)?.let { merged ->
                commands.removeLast()
                commands.addLast(HiroComposeCommand.PointerEvent(merged))
                return
            }

            commands.addLast(current)
        }
    }

    fun takeSnapshot(): List<HiroComposeCommand> = synchronized(lock) {
        if (commands.isEmpty()) return@synchronized emptyList()

        ArrayList(commands).also { commands.clear() }
    }

    fun clear() = synchronized(lock) {
        commands.forEach { command ->
            if (command is HiroComposeCommand.ViewKeyInput) command.request.cancel()
        }
        commands.clear()
    }

    fun isNotEmpty(): Boolean = synchronized(lock) { commands.isNotEmpty() }
}

internal sealed interface HiroComposeCommand {
    data class SetContent(val content: @Composable () -> Unit) : HiroComposeCommand
    data class PointerEvent(val event: HiroComposePointerEvent) : HiroComposeCommand
    data class MoveLifecycle(val state: Lifecycle.State) : HiroComposeCommand
    data class ImeEdit(val request: HiroImeEditRequest) : HiroComposeCommand
    data class ImeAction(val sessionId: Long, val action: androidx.compose.ui.text.input.ImeAction) : HiroComposeCommand
    data class ImeKeyInput(val event: KeyEvent) : HiroComposeCommand
    data class ViewKeyInput(val request: HiroViewKeyEventRequest) : HiroComposeCommand
    data object CancelPointerInput : HiroComposeCommand
    data object NavigationBack : HiroComposeCommand
    data object ReleaseFocus : HiroComposeCommand
    data class TakeFocus(val direction: FocusDirection) : HiroComposeCommand
}
