package me.earzuchan.hiro.example.compose

import android.app.Activity
import android.os.Bundle
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat.enableEdgeToEdge
import me.earzuchan.hiro.compose.setHiroComposeContent
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MambaActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(window)
        setHiroComposeContent {
            val transition = rememberInfiniteTransition(label = "loop-anime")

            val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing,), RepeatMode.Restart), "光斑相位")

            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF10131A), Color(0xFF111827), Color(0xFF172554)),))) {
                Canvas(Modifier.fillMaxSize()) {
                    val side = min(size.width, size.height)
                    val angle = phase * Math.PI.toFloat() * 2f
                    val radius = side * 0.16f
                    val firstCenter = Offset(x = size.width * 0.5f + cos(angle) * side * 0.24f, y = size.height * 0.5f + sin(angle * 0.82f) * side * 0.24f)
                    val secondCenter = Offset(x = size.width - firstCenter.x, y = size.height - firstCenter.y)

                    drawCircle(Color(0xFF38BDF8), radius, firstCenter, 0.86f)
                    drawCircle(Color(0xFFE879F9), radius * 0.72f, secondCenter, 0.78f)
                    drawCircle(Color.White, side * 0.035f, Offset(size.width * 0.5f, size.height * 0.5f), 0.72f)
                }

                Column(Modifier.fillMaxSize().padding(28.dp), Arrangement.Bottom) {
                    BasicText("Hiro Compose Showcase", style = TextStyle(Color.White, 34.sp))
                    BasicText("这是Skiko的Compose，不是AndroidX的", style = TextStyle(Color(0xCCFFFFFF), 14.sp))
                }
            }
        }
    }
}
