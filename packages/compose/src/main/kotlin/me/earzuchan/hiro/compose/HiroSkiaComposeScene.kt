package me.earzuchan.hiro.compose

import android.util.Log
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import me.earzuchan.hiro.compose.internal.HiroAndroidPlatformContext
import me.earzuchan.hiro.compose.internal.HiroAndroidUiDispatcher
import me.earzuchan.hiro.compose.internal.input.HiroComposePointerEvent
import me.earzuchan.hiro.compose.internal.windowinsets.HiroMutablePlatformWindowInsets
import org.jetbrains.skia.Canvas as SkiaCanvas

@OptIn(InternalComposeUiApi::class)
class HiroSkiaComposeScene private constructor(private val scheduleFrame: () -> Unit, density: Density, layoutDirection: LayoutDirection, windowInsets: PlatformWindowInsets) : AutoCloseable {
    constructor(scheduleFrame: () -> Unit, density: Density = Density(1f), layoutDirection: LayoutDirection = LayoutDirection.Ltr, windowInsets: HiroMutablePlatformWindowInsets = HiroMutablePlatformWindowInsets()) : this(scheduleFrame, density, layoutDirection, windowInsets as PlatformWindowInsets)

    private val dispatcher = HiroAndroidUiDispatcher
    private val platformContext = HiroAndroidPlatformContext(windowInsets)
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

        scene.setContent(content = content)
        scheduleFrame()
    }

    internal fun attachHostView(view: View) = platformContext.attachHostView(view)

    internal fun detachHostView(view: View) = platformContext.detachHostView(view)

    internal fun sendPointerEvent(event: HiroComposePointerEvent): Boolean {
        scene.sendPointerEvent(event.type, event.pointers, event.buttons, event.keyboardModifiers, event.scrollDelta, event.timeMillis, event.nativeEvent, event.changedButton, event.scaleGestureFactor, event.panGestureOffset)
        scheduleFrame()

        return true
    }

    internal fun cancelPointerInput() {
        scene.cancelPointerInput()
        scheduleFrame()
    }

    // 这个是啊一个控制反转
    fun render(canvas: SkiaCanvas, width: Int, height: Int, density: Density, layoutDirection: LayoutDirection, nanoTime: Long) {
        // TIPS：因为是渲染，可能高频，不LOG

        check(width >= 0 && height >= 0) { "Compose Skia Android 渲染尺寸不能为负数" }

        val nextSize = IntSize(width, height)

        if (currentSize != nextSize) {
            currentSize = nextSize
            scene.size = nextSize
        }

        if (currentDensity != density) {
            currentDensity = density
            scene.density = density
        }

        if (currentLayoutDirection != layoutDirection) {
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
