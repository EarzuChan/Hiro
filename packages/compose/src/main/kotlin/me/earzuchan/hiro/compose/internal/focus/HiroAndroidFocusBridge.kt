package me.earzuchan.hiro.compose.internal.focus

import android.graphics.Rect
import android.view.View
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Rect as ComposeRect
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicLong
import me.earzuchan.hiro.compose.internal.util.checkMainThreadForHiroCompose

internal class HiroAndroidFocusBridge(private val view: View) : FocusManager {
    private val nextFocusIntent = AtomicLong()
    private val focusedRect = AtomicReference<Rect?>(null)
    private var minimumValidFocusIntent = 0L
    private var onViewFocusLost: (() -> Unit)? = null
    private var suppressNextFocusEntry = false

    fun bind(onViewFocusLost: () -> Unit) {
        checkMainThreadForHiroCompose()
        check(this.onViewFocusLost == null) { "Hiro 焦点桥已经绑定" }
        this.onViewFocusLost = onViewFocusLost
    }

    fun unbind() {
        checkMainThreadForHiroCompose()
        onViewFocusLost = null
        minimumValidFocusIntent = nextFocusIntent.incrementAndGet()
        focusedRect.set(null)
    }

    fun requestViewFocus(): Boolean {
        if (!view.isAttachedToWindow) return false

        val intent = nextFocusIntent.incrementAndGet()
        return view.post {
            if (intent >= minimumValidFocusIntent && view.isAttachedToWindow) {
                suppressNextFocusEntry = true

                try {
                    view.requestFocus()
                } finally {
                    suppressNextFocusEntry = false
                }
            }
        }
    }

    fun consumeExternalFocusEntry(): Boolean {
        val shouldEnter = !suppressNextFocusEntry
        suppressNextFocusEntry = false
        return shouldEnter
    }

    fun toComposeFocusDirection(direction: Int): FocusDirection = when (direction) {
        View.FOCUS_FORWARD -> FocusDirection.Next
        View.FOCUS_BACKWARD -> FocusDirection.Previous
        View.FOCUS_UP -> FocusDirection.Up
        View.FOCUS_DOWN -> FocusDirection.Down
        View.FOCUS_LEFT -> FocusDirection.Left
        View.FOCUS_RIGHT -> FocusDirection.Right
        else -> FocusDirection.Enter
    }

    fun onViewFocusChanged(hasFocus: Boolean) {
        checkMainThreadForHiroCompose()
        if (hasFocus) return

        minimumValidFocusIntent = nextFocusIntent.incrementAndGet()
        focusedRect.set(null)
        onViewFocusLost?.invoke()
    }

    fun publishFocusedRect(rect: ComposeRect?) = focusedRect.set(rect?.let {
        Rect(
            it.left.toInt(),
            it.top.toInt(),
            it.right.toInt(),
            it.bottom.toInt(),
        )
    })

    fun copyFocusedRect(outRect: Rect): Boolean {
        val current = focusedRect.get() ?: return false
        outRect.set(current)
        return true
    }

    override fun clearFocus(force: Boolean) {
        val intent = nextFocusIntent.incrementAndGet()
        view.post { if (intent >= minimumValidFocusIntent && view.isFocused) view.clearFocus() }
    }

    override fun moveFocus(focusDirection: FocusDirection): Boolean {
        val androidDirection = focusDirection.toAndroidDirection() ?: return false
        val intent = nextFocusIntent.incrementAndGet()
        return view.post {
            if (intent < minimumValidFocusIntent || !view.isAttachedToWindow) return@post

            val next = view.focusSearch(androidDirection)
            if (next != null && next !== view) next.requestFocus(androidDirection)
        }
    }

    private fun FocusDirection.toAndroidDirection(): Int? = when (this) {
        FocusDirection.Next -> View.FOCUS_FORWARD
        FocusDirection.Previous -> View.FOCUS_BACKWARD
        FocusDirection.Up -> View.FOCUS_UP
        FocusDirection.Down -> View.FOCUS_DOWN
        FocusDirection.Left -> View.FOCUS_LEFT
        FocusDirection.Right -> View.FOCUS_RIGHT
        else -> null
    }
}
