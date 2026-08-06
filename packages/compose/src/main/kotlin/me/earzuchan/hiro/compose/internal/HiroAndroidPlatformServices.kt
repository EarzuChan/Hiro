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
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil
import kotlin.math.floor

// CHECK：这个还有别的用武之地吗，除了一个小小的Toolbar
internal class HiroAndroidPlatformServices(val view: View) {
    val applicationContext: Context = view.context.applicationContext

    fun createTextToolbar(): TextToolbar {
        val dispatcher = checkNotNull(HiroRenderDispatcherRegistry.currentRegisteredDispatcher()) { "Hiro 文本工具栏只能在已注册的渲染场景中创建" }
        return HiroAndroidTextToolbar(view, dispatcher)
    }
}

private class HiroAndroidTextToolbar(private val view: View, private val dispatcher: HiroSkiaRenderDispatcher) : TextToolbar {
    private data class Request(
        val contentRect: AndroidRect,
        val onCopyRequested: (() -> Unit)?,
        val onPasteRequested: (() -> Unit)?,
        val onCutRequested: (() -> Unit)?,
        val onSelectAllRequested: (() -> Unit)?,
        val onAutofillRequested: (() -> Unit)?,
    )

    @Volatile private var actionMode: ActionMode? = null
    @Volatile private var requested = false
    private val generation = AtomicLong()
    private var latestRequest: Request? = null
    private var currentCallback: ToolbarCallback? = null
    private var restoreRunnable: Runnable? = null
    private var motionSuppressed = false

    override val status: TextToolbarStatus get() = if (requested) TextToolbarStatus.Shown else TextToolbarStatus.Hidden

    override fun showMenu(rect: Rect, onCopyRequested: (() -> Unit)?, onPasteRequested: (() -> Unit)?, onCutRequested: (() -> Unit)?, onSelectAllRequested: (() -> Unit)?) = showMenu(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested, null)

    override fun showMenu(rect: Rect, onCopyRequested: (() -> Unit)?, onPasteRequested: (() -> Unit)?, onCutRequested: (() -> Unit)?, onSelectAllRequested: (() -> Unit)?, onAutofillRequested: (() -> Unit)?) {
        requested = true

        val currentGeneration = generation.incrementAndGet()
        val request = Request(
            contentRect = rect.toAndroidRect(),
            onCopyRequested = onCopyRequested,
            onPasteRequested = onPasteRequested,
            onCutRequested = onCutRequested,
            onSelectAllRequested = onSelectAllRequested,
            onAutofillRequested = onAutofillRequested,
        )

        view.post {
            if (currentGeneration != generation.get() || !requested || !view.isAttachedToWindow) return@post
            handleShow(request, currentGeneration)
        }
    }

    override fun hide() {
        requested = false
        generation.incrementAndGet()

        view.post {
            cancelRestore()
            latestRequest = null
            motionSuppressed = false
            finishActionMode()
        }
    }

    private fun handleShow(request: Request, currentGeneration: Long) {
        val previousRect = latestRequest?.contentRect
        latestRequest = request

        val callback = currentCallback
        if (callback != null && actionMode != null) {
            callback.update(request)
            if (previousRect != null && previousRect != request.contentRect) {
                motionSuppressed = true
                finishActionMode()
                scheduleRestore(currentGeneration)
            } else {
                actionMode?.invalidate()
                actionMode?.invalidateContentRect()
            }
            return
        }

        if (motionSuppressed) scheduleRestore(currentGeneration)
        else startActionMode(request)
    }

    private fun scheduleRestore(currentGeneration: Long) {
        cancelRestore()
        restoreRunnable = Runnable {
            restoreRunnable = null
            if (currentGeneration != generation.get() || !requested || !view.isAttachedToWindow) return@Runnable
            val request = latestRequest ?: return@Runnable
            motionSuppressed = false
            startActionMode(request)
        }.also { view.postDelayed(it, TOOLBAR_RESTORE_DELAY_MILLIS) }
    }

    private fun cancelRestore() {
        restoreRunnable?.let(view::removeCallbacks)
        restoreRunnable = null
    }

    private fun startActionMode(request: Request) {
        if (actionMode != null) return
        val callback = ToolbarCallback(request)
        currentCallback = callback
        actionMode = view.startActionMode(callback, ActionMode.TYPE_FLOATING)
        if (actionMode == null) {
            currentCallback = null
            requested = false
        }
    }

    private fun finishActionMode() {
        val current = actionMode
        actionMode = null
        currentCallback = null
        current?.finish()
    }

    private inner class ToolbarCallback(private var request: Request) : ActionMode.Callback2() {
        fun update(req: Request) {
            request = req
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu) = populateMenu(menu)

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.clear()
            populateMenu(menu)

            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val action = when (item.itemId) {
                android.R.id.copy -> request.onCopyRequested
                android.R.id.paste -> request.onPasteRequested
                android.R.id.cut -> request.onCutRequested
                android.R.id.selectAll -> request.onSelectAllRequested
                android.R.id.autofill -> request.onAutofillRequested
                else -> null
            } ?: return false

            requested = false
            generation.incrementAndGet()
            cancelRestore()
            latestRequest = null

            if (!dispatcher.tryDispatch(Runnable(action))) Log.w(TAG, "文本工具栏动作无法投递到 Hiro 渲染线程")

            mode.finish()
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            if (actionMode === mode) actionMode = null
            if (currentCallback === this) currentCallback = null
        }

        override fun onGetContentRect(mode: ActionMode, view: View, outRect: AndroidRect) {
            outRect.set(request.contentRect)
        }

        private fun populateMenu(menu: Menu): Boolean {
            request.onCopyRequested?.let { menu.add(0, android.R.id.copy, 0, android.R.string.copy) }
            request.onPasteRequested?.let { menu.add(0, android.R.id.paste, 1, android.R.string.paste) }
            request.onCutRequested?.let { menu.add(0, android.R.id.cut, 2, android.R.string.cut) }
            request.onSelectAllRequested?.let { menu.add(0, android.R.id.selectAll, 3, android.R.string.selectAll) }
            request.onAutofillRequested?.let { menu.add(0, android.R.id.autofill, 4, android.R.string.autofill) }
            return menu.isNotEmpty()
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
        private const val TOOLBAR_RESTORE_DELAY_MILLIS = 150L
    }
}