package me.earzuchan.hiro.samples.fullscreen

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.earzuchan.hiro.compose.HiroSkikoComposeScene
import me.earzuchan.hiro.skiko.HiroSkiaLayer
import me.earzuchan.hiro.skiko.HiroSkiaRenderDelegate
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class P0ComposeSceneActivity : Activity() {
    private val layer = HiroSkiaLayer()
    private var composeScene: HiroSkikoComposeScene? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)

        val root = FrameLayout(this)
        setContentView(root)

        val scene = HiroSkikoComposeScene(scheduleFrame = { layer.needRender() }, density = currentDensity(), layoutDirection = currentLayoutDirection()).apply { setContent { P0ComposeSceneSample() } }
        composeScene = scene

        layer.renderDelegate = HiroSkiaRenderDelegate { canvas, width, height, nanoTime -> scene.render(canvas = canvas, width = width, height = height, density = currentDensity(), layoutDirection = currentLayoutDirection(), nanoTime = nanoTime) }

        layer.attachTo(root).contentDescription = "Hiro P0.3 Compose Skiko 画布"
    }

    override fun onResume() {
        super.onResume()
        layer.onHostResume()
        layer.needRender()
    }

    override fun onPause() {
        layer.onHostPause()
        super.onPause()
    }

    override fun onDestroy() {
        composeScene?.close()
        composeScene = null
        layer.close()
        super.onDestroy()
    }

    private fun currentDensity() = Density(density = resources.displayMetrics.density, fontScale = resources.configuration.fontScale)

    private fun currentLayoutDirection() = if (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr
}

@Composable
private fun P0ComposeSceneSample() {
    val transition = rememberInfiniteTransition(label = "P0.3 循环动画")

    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2400,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "光斑相位",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF10131A),
                        Color(0xFF111827),
                        Color(0xFF172554),
                    ),
                ),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = min(size.width, size.height)
            val angle = phase * Math.PI.toFloat() * 2f
            val radius = side * 0.16f
            val firstCenter = Offset(x = size.width * 0.5f + cos(angle) * side * 0.24f, y = size.height * 0.5f + sin(angle * 0.82f) * side * 0.24f)
            val secondCenter = Offset(x = size.width - firstCenter.x, y = size.height - firstCenter.y)

            drawCircle(
                color = Color(0xFF38BDF8),
                radius = radius,
                center = firstCenter,
                alpha = 0.86f,
            )
            drawCircle(
                color = Color(0xFFE879F9),
                radius = radius * 0.72f,
                center = secondCenter,
                alpha = 0.78f,
            )
            drawCircle(
                color = Color.White,
                radius = side * 0.035f,
                center = Offset(size.width * 0.5f, size.height * 0.5f),
                alpha = 0.72f,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            BasicText(
                text = "Hiro P0.3",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 34.sp,
                ),
            )
            BasicText(
                text = "CanvasLayersComposeScene / ComposeSceneRecomposer / SkiaBackedCanvas",
                style = TextStyle(
                    color = Color(0xCCFFFFFF),
                    fontSize = 14.sp,
                ),
            )
        }
    }
}
