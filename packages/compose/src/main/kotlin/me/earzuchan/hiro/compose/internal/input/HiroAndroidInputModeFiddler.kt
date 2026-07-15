package me.earzuchan.hiro.compose.internal.input

import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.ui.input.InputMode

internal class HiroAndroidInputModeFiddler(private val onInputModeChanged: (InputMode) -> Unit) : AutoCloseable {
    private var hostView: View? = null
    private var treeObserver: ViewTreeObserver? = null
    private val listener = ViewTreeObserver.OnTouchModeChangeListener { publish(it) }

    fun attach(view: View) {
        check(hostView == null) { "Hiro Android 输入模式桥已经挂载" }
        hostView = view
        treeObserver = view.viewTreeObserver.also { it.addOnTouchModeChangeListener(listener) }
        publish(view.isInTouchMode)
    }

    fun request(inputMode: InputMode): Boolean {
        val view = hostView ?: return false

        return when (inputMode) {
            InputMode.Touch -> view.isInTouchMode
            InputMode.Keyboard -> if (view.isInTouchMode) view.requestFocusFromTouch() else true
            else -> false
        }.also { publish(view.isInTouchMode) }
    }

    override fun close() {
        treeObserver?.takeIf { it.isAlive }?.removeOnTouchModeChangeListener(listener)
        treeObserver = null
        hostView = null
    }

    private fun publish(isInTouchMode: Boolean) = onInputModeChanged(if (isInTouchMode) InputMode.Touch else InputMode.Keyboard)
}
