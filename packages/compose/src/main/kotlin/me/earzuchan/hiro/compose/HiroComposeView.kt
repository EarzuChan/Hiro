package me.earzuchan.hiro.compose

import android.content.Context
import android.content.res.Configuration
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import me.earzuchan.hiro.compose.internal.HiroComposeHostSession
import me.earzuchan.hiro.compose.internal.architecture.HiroSavedStateTransport
import me.earzuchan.hiro.compose.savable.HiroSavableStateConfiguration

class HiroComposeView private constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    private val configuration: HiroComposeConfiguration,
    private val explicitSavedStateKey: String?,
) : FrameLayout(context, attrs, defStyleAttr), AutoCloseable {
    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : this(
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

    constructor(context: Context, savableStateConfiguration: HiroSavableStateConfiguration) : this(
        context = context,
        configuration = hiroComposeConfiguration { savableState(savableStateConfiguration) },
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
        layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        isFocusable = true
        isFocusableInTouchMode = true
        Log.d(TAG, "被创建")
    }

    fun setContent(content: @Composable () -> Unit) {
        checkMainThread()
        check(!closed) { "HiroComposeView 已经永久关闭，不能再设置 Compose 内容" }
        this.content = content
        activeSession?.setContent(content)
        Log.d(TAG, "已接收 Compose 内容")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        checkMainThread()
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
        checkMainThread()
        disposeActiveSession()
        Log.d(TAG, "已从窗口脱离并销毁宿主会话")
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        activeSession?.synchronizeLifecycle()
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
        if (event.actionMasked == MotionEvent.ACTION_DOWN) requestFocus()

        val handled = activeSession?.dispatchTouchEvent(event) == true
        if (event.shouldLogTouchEvent()) Log.d(TAG, "触摸事件：${event.name()}，指针数：${event.pointerCount}，动作指针：${event.actionIndex}，已处理：$handled")
        return handled || super.dispatchTouchEvent(event)
    }

    override fun close() {
        checkMainThread()
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

    private fun checkMainThread() = check(Looper.myLooper() == Looper.getMainLooper()) { "HiroComposeView 只能在安卓主线程操作" }
}

private fun MotionEvent.shouldLogTouchEvent() = when (actionMasked) {
    MotionEvent.ACTION_DOWN,
    MotionEvent.ACTION_UP,
    MotionEvent.ACTION_POINTER_DOWN,
    MotionEvent.ACTION_POINTER_UP,
    MotionEvent.ACTION_CANCEL,
    MotionEvent.ACTION_OUTSIDE -> true
    else -> false
}

private fun MotionEvent.name() = when (actionMasked) {
    MotionEvent.ACTION_DOWN -> "按下"
    MotionEvent.ACTION_UP -> "抬起"
    MotionEvent.ACTION_POINTER_DOWN -> "多指按下"
    MotionEvent.ACTION_POINTER_UP -> "多指抬起"
    MotionEvent.ACTION_CANCEL -> "取消"
    MotionEvent.ACTION_OUTSIDE -> "越界"
    else -> "其他"
}
