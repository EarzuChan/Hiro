package me.earzuchan.hiro.compose.internal

import android.content.Context
import android.graphics.Rect as AndroidRect
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.core.view.isNotEmpty
import kotlin.math.ceil
import kotlin.math.floor

internal class HiroAndroidPlatformServices(val view: View) {
    val applicationContext: Context = view.context.applicationContext

    fun createTextToolbar(): TextToolbar {
        val dispatcher = checkNotNull(HiroRenderDispatcherRegistry.currentRegisteredDispatcher()) {
            "Hiro 文本工具栏只能在已注册的渲染场景中创建"
        }
        return HiroAndroidTextToolbar(view, dispatcher)
    }
}

private class HiroAndroidTextToolbar(
    private val view: View,
    private val dispatcher: HiroSkiaRenderDispatcher,
) : TextToolbar {
    @Volatile private var actionMode: ActionMode? = null
    @Volatile private var generation = 0L

    override val status: TextToolbarStatus
        get() = if (actionMode == null) TextToolbarStatus.Hidden else TextToolbarStatus.Shown

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) = showMenu(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested, null)

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
        onAutofillRequested: (() -> Unit)?,
    ) {
        val currentGeneration = ++generation
        view.post {
            if (currentGeneration != generation || !view.isAttachedToWindow) return@post
            actionMode?.finish()
            val callback = ToolbarCallback(
                contentRect = rect.toAndroidRect(),
                onCopyRequested = onCopyRequested,
                onPasteRequested = onPasteRequested,
                onCutRequested = onCutRequested,
                onSelectAllRequested = onSelectAllRequested,
                onAutofillRequested = onAutofillRequested,
                onDestroyed = { destroyed -> if (actionMode === destroyed) actionMode = null },
            )
            actionMode = view.startActionMode(callback, ActionMode.TYPE_FLOATING)
        }
    }

    override fun hide() {
        generation++
        view.post {
            actionMode?.finish()
            actionMode = null
        }
    }

    private inner class ToolbarCallback(
        private val contentRect: AndroidRect,
        private val onCopyRequested: (() -> Unit)?,
        private val onPasteRequested: (() -> Unit)?,
        private val onCutRequested: (() -> Unit)?,
        private val onSelectAllRequested: (() -> Unit)?,
        private val onAutofillRequested: (() -> Unit)?,
        private val onDestroyed: (ActionMode) -> Unit,
    ) : ActionMode.Callback2() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            onCopyRequested?.let { menu.add(0, android.R.id.copy, 0, android.R.string.copy) }
            onPasteRequested?.let { menu.add(0, android.R.id.paste, 1, android.R.string.paste) }
            onCutRequested?.let { menu.add(0, android.R.id.cut, 2, android.R.string.cut) }
            onSelectAllRequested?.let { menu.add(0, android.R.id.selectAll, 3, android.R.string.selectAll) }
            onAutofillRequested?.let { menu.add(0, android.R.id.autofill, 4, android.R.string.autofill) }
            return menu.isNotEmpty()
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val action = when (item.itemId) {
                android.R.id.copy -> onCopyRequested
                android.R.id.paste -> onPasteRequested
                android.R.id.cut -> onCutRequested
                android.R.id.selectAll -> onSelectAllRequested
                android.R.id.autofill -> onAutofillRequested
                else -> null
            } ?: return false
            if (!dispatcher.tryDispatch(Runnable(action))) {
                Log.w(TAG, "文本工具栏动作无法投递到 Hiro 渲染线程")
            }
            mode.finish()
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) = onDestroyed(mode)

        override fun onGetContentRect(mode: ActionMode, view: View, outRect: AndroidRect) {
            outRect.set(contentRect)
        }
    }

    private fun Rect.toAndroidRect() = AndroidRect(
        floor(left).toInt(),
        floor(top).toInt(),
        ceil(right).toInt(),
        ceil(bottom).toInt(),
    )

    companion object {
        private const val TAG = "HiroTextToolbar"
    }
}
