package me.earzuchan.hiro.compose

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import me.earzuchan.hiro.compose.internal.HiroAndroidPlatformContext
import me.earzuchan.hiro.compose.internal.HiroAndroidUiDispatcher
import org.jetbrains.skia.Canvas as SkiaCanvas

@OptIn(InternalComposeUiApi::class)
class HiroSkikoComposeScene(private val scheduleFrame: () -> Unit, density: Density = Density(1f), layoutDirection: LayoutDirection = LayoutDirection.Ltr) : AutoCloseable {
    private val dispatcher = HiroAndroidUiDispatcher
    private val platformContext = HiroAndroidPlatformContext()
    private val scene = CanvasLayersComposeScene(density = density, layoutDirection = layoutDirection, coroutineContext = dispatcher, platformContext = platformContext, invalidate = scheduleFrame)
    private var currentSize: IntSize? = null
    private var currentDensity: Density = density
    private var currentLayoutDirection: LayoutDirection = layoutDirection

    companion object {
        private const val TAG = "HiroSkikoComposeScene"
    }

    init {
        Log.d(TAG,"被创建呢啊一个有感觉吗")
    }

    fun setContent(content: @Composable () -> Unit) {
        Log.d(TAG,"啊被设了内容，好喜欢哥哥的大内容")

        scene.setContent(content = content)
        scheduleFrame()
    }

    // 这个是啊一个控制反转
    fun render(canvas: SkiaCanvas, width: Int, height: Int, density: Density, layoutDirection: LayoutDirection, nanoTime: Long) {
        // TIPS：因为是渲染，可能高频，不LOG

        check(width >= 0 && height >= 0) { "Compose Skiko Android 渲染尺寸不能为负数" }

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

        Log.d(TAG,"啊被关闭了一个呢")
    }
}
