package me.earzuchan.hiro.example.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeShader
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.earzuchan.hiro.compose.setHiroComposeContent
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MambaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setHiroComposeContent {
            val transition = rememberInfiniteTransition(label = "loop-anime")

            val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), "光斑相位")

            Box(Modifier.fillMaxSize()) {
                SkSlBackground(phase)

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

                Column(Modifier.fillMaxSize().safeContentPadding().border(1.dp, Color.Red).padding(28.dp), Arrangement.Bottom) {
                    BasicText("Hiro Compose Showcase", style = TextStyle(Color.White, 34.sp))
                    BasicText("这是Hiro的Compose（By Skia），不是AndroidX的", style = TextStyle(Color(0xCCFFFFFF), 14.sp))
                }
            }
        }
    }
}

@Composable
private fun SkSlBackground(phase: Float) {
    val effect = remember {
        RuntimeEffect.makeForShader(
            """
            uniform vec2 iResolution;
            uniform float iTime;

            vec4 main(vec2 coord) {
                vec2 uv = coord / iResolution;
                vec2 p = (coord * 2.0 - iResolution) / min(iResolution.x, iResolution.y);

                float radius = length(p);
                float angle = atan(p.y, p.x);
                float wave = 0.5 + 0.5 * cos(14.0 * radius - iTime * 4.0 + angle * 3.0);
                float glow = smoothstep(0.95, 0.0, radius);

                vec3 base = mix(vec3(0.03, 0.05, 0.10), vec3(0.08, 0.13, 0.32), uv.y);
                vec3 blue = vec3(0.10, 0.70, 1.00);
                vec3 pink = vec3(0.90, 0.28, 1.00);
                vec3 color = base + blue * wave * glow * 0.55 + pink * pow(glow, 3.0) * 0.55;

                return vec4(color, 1.0);
            }
            """.trimIndent()
        )
    }

    Canvas(Modifier.fillMaxSize()) {
        val shader = RuntimeShaderBuilder(effect).apply {
            uniform("iResolution", size.width, size.height)
            uniform("iTime", phase * Math.PI.toFloat() * 2f)
        }.makeShader().asComposeShader()

        drawRect(brush = ShaderBrush(shader), size = size)
    }
}
