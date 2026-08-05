package me.earzuchan.hiro.compose.internal

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import me.earzuchan.hiro.compose.internal.input.HiroComposePointerEvent
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

    fun clear() = synchronized(lock) { commands.clear() }
}

internal sealed interface HiroComposeCommand {
    data class SetContent(val content: @Composable () -> Unit) : HiroComposeCommand
    data class PointerEvent(val event: HiroComposePointerEvent) : HiroComposeCommand
    data class MoveLifecycle(val state: Lifecycle.State) : HiroComposeCommand
    data class ImeEdit(val sessionId: Long, val commands: List<EditCommand>) : HiroComposeCommand
    data class ImeAction(val sessionId: Long, val action: androidx.compose.ui.text.input.ImeAction) : HiroComposeCommand
    data class KeyInput(val event: KeyEvent) : HiroComposeCommand
    data object CancelPointerInput : HiroComposeCommand
    data object NavigationBack : HiroComposeCommand
}
