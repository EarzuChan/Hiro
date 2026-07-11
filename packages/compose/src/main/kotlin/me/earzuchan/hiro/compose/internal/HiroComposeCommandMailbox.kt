package me.earzuchan.hiro.compose.internal

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import me.earzuchan.hiro.compose.internal.input.HiroComposePointerEvent
import me.earzuchan.hiro.compose.internal.input.coalesceMoveWith
import java.util.ArrayDeque

internal class HiroComposeCommandMailbox {
    private val lock = Any()
    private val commands = ArrayDeque<HiroComposeCommand>()

    fun add(command: HiroComposeCommand) {
        synchronized(lock) {
            val previous = commands.peekLast()
            if (previous is HiroComposeCommand.PointerEvent && command is HiroComposeCommand.PointerEvent) {
                previous.event.coalesceMoveWith(command.event)?.let { merged ->
                    commands.removeLast()
                    commands.addLast(HiroComposeCommand.PointerEvent(merged))
                    return
                }
            }
            commands.addLast(command)
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
    data object ApplyEnvironment : HiroComposeCommand
    data object ApplyViewport : HiroComposeCommand
    data object ApplyWindowInsets : HiroComposeCommand
    data object ApplyInputMode : HiroComposeCommand
    data class PointerEvent(val event: HiroComposePointerEvent) : HiroComposeCommand
    data class MoveLifecycle(val state: Lifecycle.State) : HiroComposeCommand
    data object CancelPointerInput : HiroComposeCommand
    data object NavigationBack : HiroComposeCommand
}
