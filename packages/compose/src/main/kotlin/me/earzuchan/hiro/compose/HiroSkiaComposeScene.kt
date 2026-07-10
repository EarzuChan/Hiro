package me.earzuchan.hiro.compose

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import me.earzuchan.hiro.compose.internal.HiroAndroidPlatformContext
import me.earzuchan.hiro.compose.internal.HiroComposeEnvironment
import me.earzuchan.hiro.compose.internal.HiroSkiaRenderDispatcher
import me.earzuchan.hiro.compose.internal.architecture.HiroSavedStateTransport
import me.earzuchan.hiro.compose.internal.input.HiroComposePointerEvent
import me.earzuchan.hiro.compose.internal.windowinsets.HiroMutablePlatformWindowInsets
import me.earzuchan.hiro.compose.internal.windowinsets.HiroPlatformWindowInsetsSnapshot
import org.jetbrains.skia.Canvas as SkiaCanvas

@OptIn(InternalComposeUiApi::class)
class HiroSkiaComposeScene internal constructor(private val scheduleFrame: () -> Unit, private val dispatcher: HiroSkiaRenderDispatcher, initialEnvironment: HiroComposeEnvironment, private val requestInputMode: (InputMode) -> Boolean, requestNavigationBackHandling: (Boolean) -> Boolean, savedStateTransport: HiroSavedStateTransport) : AutoCloseable {
    private val systemTheme = mutableStateOf(initialEnvironment.systemTheme)

    private val windowInsets = HiroMutablePlatformWindowInsets()

    private val platformContext = HiroAndroidPlatformContext(hiroWindowInsets = windowInsets as PlatformWindowInsets, requestInputMode = requestInputMode, requestNavigationBackHandling = requestNavigationBackHandling, savedStateTransport = savedStateTransport)

    private val lifecycleOwner = checkNotNull(platformContext.architectureComponentsOwner.lifecycleOwner) { "Hiro Compose 没有可用的 LifecycleOwner" }

    private val viewModelStoreOwner = checkNotNull(platformContext.architectureComponentsOwner.viewModelStoreOwner) { "Hiro Compose 没有可用的 ViewModelStoreOwner" }

    private val navigationEventDispatcherOwner = platformContext.architectureComponentsOwner.navigationEventDispatcherOwner

    private val savedStateRegistryOwner = checkNotNull(platformContext.architectureComponentsOwner.savedStateRegistryOwner) { "Hiro Compose 没有可用的 SavedStateRegistryOwner" }

    private val scene: ComposeScene = CanvasLayersComposeScene(
        density = initialEnvironment.density,
        layoutDirection = initialEnvironment.layoutDirection,
        coroutineContext = dispatcher,
        platformContext = platformContext,
        invalidate = scheduleFrame,
    )

    private var currentSize: IntSize? = null
    private var currentEnvironment = initialEnvironment
    private var closed = false

    companion object {
        private const val TAG = "HiroSkiaComposeScene"
    }

    init {
        checkRenderThread()
        Log.d(TAG, "已在渲染线程创建")
    }

    fun setContent(content: @Composable () -> Unit) {
        checkUsable()

        scene.setContent {
            CompositionLocalProvider(
                LocalSystemTheme provides systemTheme.value,
                LocalLifecycleOwner provides lifecycleOwner,
                LocalViewModelStoreOwner provides viewModelStoreOwner,
                LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner,
                LocalSavedStateRegistryOwner provides savedStateRegistryOwner,
            ) { content() }
        }

        scheduleFrame()
    }

    internal fun updateEnvironment(environment: HiroComposeEnvironment) {
        checkUsable()

        if (currentEnvironment.density != environment.density) scene.density = environment.density
        if (currentEnvironment.layoutDirection != environment.layoutDirection) scene.layoutDirection = environment.layoutDirection
        if (currentEnvironment.systemTheme != environment.systemTheme) systemTheme.value = environment.systemTheme
        currentEnvironment = environment
        scheduleFrame()
    }

    internal fun updateWindowInsets(snapshot: HiroPlatformWindowInsetsSnapshot) {
        checkUsable()

        if (windowInsets.update(snapshot)) scheduleFrame()
    }

    internal fun updateInputMode(inputMode: InputMode) {
        checkUsable()

        platformContext.updateInputMode(inputMode)
        scheduleFrame()
    }

    internal fun moveLifecycleTo(state: Lifecycle.State) {
        checkUsable()

        platformContext.moveLifecycleTo(state)
        scheduleFrame()
    }

    internal fun checkpointSavedState() {
        checkUsable()

        platformContext.checkpointSavedState()
    }

    internal fun dispatchNavigationBack() {
        checkUsable()

        platformContext.dispatchNavigationBack()
        scheduleFrame()
    }

    internal fun sendPointerEvent(event: HiroComposePointerEvent): Boolean {
        checkUsable()

        if (event.type != PointerEventType.Move) {
            val historicalCount = event.pointers.sumOf { it.historical.size }
            Log.d(TAG, "指针事件注入：${event.type.name()}，指针数：${event.pointers.size}，历史点：$historicalCount")
        }

        scene.sendPointerEvent(
            event.type,
            event.pointers,
            event.buttons,
            event.keyboardModifiers,
            event.scrollDelta,
            event.timeMillis,
            event.nativeEvent,
            event.changedButton,
            event.scaleGestureFactor,
            event.panGestureOffset,
        )

        scheduleFrame()
        return true
    }

    internal fun cancelPointerInput() {
        checkUsable()
        scene.cancelPointerInput()
        scheduleFrame()
    }

    @SuppressLint("RestrictedApi")
    fun render(canvas: SkiaCanvas, width: Int, height: Int, nanoTime: Long) {
        checkUsable()
        check(width >= 0 && height >= 0) { "Compose Skia Android 渲染尺寸不能为负数" }

        val nextSize = IntSize(width, height)
        if (currentSize != nextSize) {
            currentSize = nextSize
            scene.size = nextSize
            Log.d(TAG, "渲染尺寸改变：${nextSize.width}x${nextSize.height}")
        }

        scene.render(canvas.asComposeCanvas(), nanoTime)
    }

    override fun close() {
        checkRenderThread()

        if (closed) return
        closed = true
        platformContext.use { scene.close() }
        Log.d(TAG, "已在渲染线程关闭")
    }

    private fun checkUsable() {
        checkRenderThread()
        check(!closed) { "HiroSkiaComposeScene 已经关闭" }
    }

    private fun checkRenderThread() = check(dispatcher.isOnRenderThread()) { "HiroSkiaComposeScene 只能在 Skia 渲染线程操作" }
}

private fun PointerEventType.name() = when (this) {
    PointerEventType.Press -> "按下"
    PointerEventType.Release -> "抬起"
    PointerEventType.Move -> "移动"
    PointerEventType.Enter -> "进入"
    PointerEventType.Exit -> "离开"
    PointerEventType.Scroll -> "滚动"
    else -> "未知"
}
