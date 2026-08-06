package me.earzuchan.hiro.compose

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.util.Log
import android.graphics.Rect
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import me.earzuchan.hiro.compose.internal.HiroComposeHostSession
import me.earzuchan.hiro.compose.internal.architecture.HiroSavedStateTransport
import me.earzuchan.hiro.compose.internal.util.checkMainThreadForHiroCompose
import me.earzuchan.hiro.compose.internal.util.name
import me.earzuchan.hiro.compose.internal.util.shouldLogTouchEvent

class HiroComposeView private constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, private val configuration: HiroComposeConfiguration, private val explicitSavedStateKey: String?) : FrameLayout(context, attrs, defStyleAttr), AutoCloseable {
    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : this( // TIPS：这是特制的
        context = context,
        attrs = attrs,
        defStyleAttr = defStyleAttr,
        configuration = HiroComposeConfiguration.DEFAULT,
        explicitSavedStateKey = null,
    )

    @JvmOverloads
    constructor(context: Context, configuration: HiroComposeConfiguration, savedStateKey: String? = null) : this(
        context = context,
        attrs = null,
        defStyleAttr = 0,
        configuration = configuration,
        explicitSavedStateKey = savedStateKey,
    )

    private val savedStateTransport = HiroSavedStateTransport()
    private var content: (@Composable () -> Unit)? = null
    private var activeSession: HiroComposeHostSession? = null
    private var savedStateKeyResolved = false
    private var resolvedSavedStateKey: String? = null
    private var closed = false

    companion object {
        private const val TAG = "HiroComposeView"
    }

    init {
        require(explicitSavedStateKey == null || explicitSavedStateKey.isNotBlank()) { "HiroComposeView 的 SavedState key 不能为空" }
        isFocusable = true
        isFocusableInTouchMode = true
        descendantFocusability = FOCUS_BLOCK_DESCENDANTS
        Log.d(TAG, "被创建")
    }

    fun setContent(content: @Composable () -> Unit) {
        checkMainThreadForHiroCompose()
        check(!closed) { "HiroComposeView 已经永久关闭，不能再设置 Compose 内容" }
        this.content = content
        activeSession?.setContent(content)
        Log.d(TAG, "已接收 Compose 内容")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        checkMainThreadForHiroCompose()
        check(!closed) { "已经永久关闭的 HiroComposeView 不能重新挂载" }
        check(activeSession == null) { "HiroComposeView 出现重复宿主会话" }

        val session = HiroComposeHostSession(
            view = this,
            configuration = configuration,
            savedStateKey = resolveSavedStateKey(),
            savedStateTransport = savedStateTransport,
            onHostDestroyed = ::close,
        )
        activeSession = session

        try {
            content?.let(session::setContent)
            session.attach()
        } catch (throwable: Throwable) {
            if (activeSession === session) activeSession = null
            session.close()
            throw throwable
        }

        if (!closed) Log.d(TAG, "已挂载到窗口并创建宿主会话")
    }

    override fun onDetachedFromWindow() {
        checkMainThreadForHiroCompose()
        disposeActiveSession()
        Log.d(TAG, "已从窗口脱离并销毁宿主会话")
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        activeSession?.synchronizeLifecycle()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        activeSession?.updateEnvironment()
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        activeSession?.onViewFocusChanged(gainFocus)
    }

    override fun getFocusedRect(rect: Rect) {
        if (activeSession?.copyFocusedRect(rect) == true) return
        super.getFocusedRect(rect)
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        val wasFocused = isFocused
        val focused = super.requestFocus(direction, previouslyFocusedRect)
        if (focused && !wasFocused) activeSession?.onPlatformFocusRequested(direction, previouslyFocusedRect)
        return focused
    }

    override fun onVisibilityChanged(changedView: android.view.View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        activeSession?.synchronizeLifecycle()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        activeSession?.updateEnvironment()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        activeSession?.updateViewport(width, height)
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(layoutDirection)
        activeSession?.updateEnvironment()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (closed) return false
        if (event.actionMasked == MotionEvent.ACTION_DOWN) activeSession?.requestViewFocusFromPointer() ?: requestFocus()

        val handled = activeSession?.dispatchTouchEvent(event) == true
        if (event.shouldLogTouchEvent()) Log.d(TAG, "触摸事件：${event.name()}，指针数：${event.pointerCount}，动作指针：${event.actionIndex}，已处理：$handled")
        return handled || super.dispatchTouchEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (closed || event.keyCode == KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event)
        return activeSession?.dispatchKeyEvent(event) == true || super.dispatchKeyEvent(event)
    }

    override fun onCheckIsTextEditor(): Boolean = activeSession?.isTextEditor() == true

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? = activeSession?.createInputConnection(outAttrs)

    override fun close() {
        checkMainThreadForHiroCompose()
        if (closed) return
        closed = true
        disposeActiveSession()
        Log.d(TAG, "已永久关闭")
    }

    private fun disposeActiveSession() {
        val session = activeSession ?: return
        activeSession = null
        session.close()
    }

    private fun resolveSavedStateKey(): String? {
        if (savedStateKeyResolved) return resolvedSavedStateKey

        resolvedSavedStateKey = explicitSavedStateKey ?: id.takeIf { it != NO_ID }?.let { "view-id:$it" }

        savedStateKeyResolved = true
        return resolvedSavedStateKey
    }
}
