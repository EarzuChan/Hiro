package me.earzuchan.hiro.compose

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import me.earzuchan.hiro.compose.internal.HiroAndroidPlatformContext
import me.earzuchan.hiro.compose.internal.HiroAndroidUiDispatcher
import me.earzuchan.hiro.compose.internal.input.HiroComposePointerEvent
import me.earzuchan.hiro.compose.internal.windowinsets.HiroMutablePlatformWindowInsets
import org.jetbrains.skia.Canvas as SkiaCanvas

@OptIn(InternalComposeUiApi::class)
class HiroSkiaComposeScene private constructor(private val scheduleFrame: () -> Unit, density: Density, layoutDirection: LayoutDirection, windowInsets: PlatformWindowInsets, private val systemTheme: State<SystemTheme>) : AutoCloseable {
    constructor(scheduleFrame: () -> Unit, density: Density = Density(1f), layoutDirection: LayoutDirection = LayoutDirection.Ltr, windowInsets: HiroMutablePlatformWindowInsets = HiroMutablePlatformWindowInsets(), systemTheme: State<SystemTheme> = mutableStateOf(SystemTheme.Unknown)) : this(scheduleFrame, density, layoutDirection, windowInsets as PlatformWindowInsets, systemTheme)

    private val dispatcher = HiroAndroidUiDispatcher
    private val platformContext = HiroAndroidPlatformContext(windowInsets)
    private val viewModelStoreOwner = checkNotNull(platformContext.architectureComponentsOwner.viewModelStoreOwner) { "Hiro Compose 没有可用的 ViewModelStoreOwner" }
    private val navigationEventDispatcherOwner = platformContext.architectureComponentsOwner.navigationEventDispatcherOwner
    private val scene = CanvasLayersComposeScene(density = density, layoutDirection = layoutDirection, coroutineContext = dispatcher, platformContext = platformContext, invalidate = scheduleFrame)
    private var currentSize: IntSize? = null
    private var currentDensity: Density = density
    private var currentLayoutDirection: LayoutDirection = layoutDirection

    companion object {
        private const val TAG = "HiroSkiaComposeScene"
    }

    init {
        Log.d(TAG, "被创建")
    }

    fun setContent(content: @Composable () -> Unit) {
        Log.d(TAG, "被设了内容")

        scene.setContent {
            CompositionLocalProvider(
                LocalSystemTheme provides systemTheme.value,
                LocalViewModelStoreOwner provides viewModelStoreOwner,
                LocalNavigationEventDispatcherOwner provides navigationEventDispatcherOwner,
            ) { content() }
        }

        scheduleFrame()
    }

    internal fun attachHostView(view: View) = platformContext.attachHostView(view)

    internal fun detachHostView(view: View) = platformContext.detachHostView(view)

    internal fun sendPointerEvent(event: HiroComposePointerEvent): Boolean {
        if (event.type != PointerEventType.Move) {
            val historicalCount = event.pointers.sumOf { it.historical.size }
            Log.d(TAG, "指针事件注入：${event.type.name()}，指针数：${event.pointers.size}，历史点：$historicalCount")
        }

        scene.sendPointerEvent(event.type, event.pointers, event.buttons, event.keyboardModifiers, event.scrollDelta, event.timeMillis, event.nativeEvent, event.changedButton, event.scaleGestureFactor, event.panGestureOffset)
        scheduleFrame()

        return true
    }

    internal fun cancelPointerInput() {
        Log.d(TAG, "指针输入取消")

        scene.cancelPointerInput()
        scheduleFrame()
    }

    // 这个是啊一个控制反转
    @SuppressLint("RestrictedApi")
    fun render(canvas: SkiaCanvas, width: Int, height: Int, density: Density, layoutDirection: LayoutDirection, nanoTime: Long) {
        // TIPS：因为是渲染，可能高频，不LOG

        check(width >= 0 && height >= 0) { "Compose Skia Android 渲染尺寸不能为负数" }

        val nextSize = IntSize(width, height)

        if (currentSize != nextSize) {
            Log.d(TAG, "渲染尺寸改变：${nextSize.width}x${nextSize.height}")
            currentSize = nextSize
            scene.size = nextSize
        }

        if (currentDensity != density) {
            Log.d(TAG, "渲染密度改变：density=${density.density}，fontScale=${density.fontScale}")
            currentDensity = density
            scene.density = density
        }

        if (currentLayoutDirection != layoutDirection) {
            Log.d(TAG, "布局方向改变：$layoutDirection")
            currentLayoutDirection = layoutDirection
            scene.layoutDirection = layoutDirection
        }

        scene.render(canvas = canvas.asComposeCanvas(), nanoTime = nanoTime)
    }

    override fun close() {
        scene.close()
        platformContext.close()

        Log.d(TAG, "被关闭")
    }
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
