package me.earzuchan.hiro.material3.ripple

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asComposeShader
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object HiroRippleShader {
    private const val DOKI_DOKI_RADIUS_FACTOR = 2.3f
    private const val KIRA_KIRA_SCALE = 2.1f
    private const val KIRA_KIRA_WIDTH = 0.05f
    private const val KIRA_KIRA_STRENGTH = 1f
    private const val MAHOU_SCALE = 1.5f
    private const val PI_ROTATE_RIGHT = PI * 0.0078125
    private const val PI_ROTATE_LEFT = PI * -0.0078125
    private val KIRA_KIRA_COLOR = Color(0x8DFFFFFF)

    private val effect = RuntimeEffect.makeForShader(
        """
        uniform vec2 in_origin;
        uniform vec2 in_touch;
        uniform float in_progress;
        uniform float in_maxRadius;
        uniform vec2 in_resolutionScale;
        uniform vec2 in_kiraKiraScale;
        uniform float in_kiraKiraPhase;
        uniform float in_kiraKiraWidth;
        uniform float in_kiraKiraStrength;
        uniform float in_mahouPhase;
        uniform vec2 in_mahouCircle1;
        uniform vec2 in_mahouCircle2;
        uniform vec2 in_mahouCircle3;
        uniform vec2 in_mahouRotation1;
        uniform vec2 in_mahouRotation2;
        uniform vec2 in_mahouRotation3;
        uniform vec4 in_color;
        uniform vec4 in_kiraKiraColor;

        float triangleKiraKira(vec2 n) {
          n  = fract(n * vec2(5.3987, 5.4421));
          n += dot(n.yx, n.xy + vec2(21.5351, 14.3137));
          float xy = n.x * n.y;
          return fract(xy * 95.4307) + fract(xy * 75.04961) - 1.0;
        }

        const float PI = 3.1415926535897932384626;

        float threshold(float v, float l, float h) {
            return step(l, v) * (1.0 - step(h, v));
        }

        float kiraKira(vec2 uv, float t) {
          float n = triangleKiraKira(uv);
          float s = 0.0;
          for (float i = 0.0; i < 4.0; i += 1.0) {
            float l = i * 0.1;
            float h = l + 0.05;
            float o = sin(PI * (t + 0.35 * i));
            s += threshold(n + o, l, h);
          }
          return saturate(s) * in_kiraKiraColor.a * in_kiraKiraStrength;
        }

        float dokiDoki(vec2 uv, vec2 xy, float radius, float softness) {
          float softnessHalf = softness * 0.5;
          float d = distance(uv, xy);
          return 1.0 - smoothstep(1.0 - softnessHalf, 1.0 + softnessHalf, d / radius);
        }

        float kiraKiraMask(vec2 uv, vec2 xy, float radius, float progress, float width, float softness) {
          float currentRadius = radius * progress;
          float outerDokiDoki = dokiDoki(uv, xy, currentRadius + width, softness);
          float innerDokiDoki = dokiDoki(uv, xy, max(currentRadius - width, 0.0), softness);
          return saturate(outerDokiDoki - innerDokiDoki);
        }

        float subProgress(float start, float end, float progress) {
            float sub = clamp(progress, start, end);
            return (sub - start) / (end - start);
        }

        mat2 rotate2d(vec2 rad) {
          return mat2(rad.x, -rad.y, rad.y, rad.x);
        }

        float mahouCircle(vec2 resolution, vec2 coord, float time, vec2 center, vec2 rotation, float cellDiameter) {
          coord = rotate2d(rotation) * (center - coord) + center;
          coord = mod(coord, cellDiameter) / resolution;
          float normalRadius = cellDiameter / resolution.y * 0.5;
          float radius = 0.65 * normalRadius;
          return dokiDoki(coord, vec2(normalRadius), radius, radius * 50.0);
        }

        float mahou(vec2 uv, float t) {
          const vec2 scale = vec2(0.8);
          uv = uv * scale;
          float g1 = mahouCircle(scale, uv, t, in_mahouCircle1, in_mahouRotation1, 0.17);
          float g2 = mahouCircle(scale, uv, t, in_mahouCircle2, in_mahouRotation2, 0.2);
          float g3 = mahouCircle(scale, uv, t, in_mahouCircle3, in_mahouRotation3, 0.275);
          float v = (g1 * g1 + g2 - g3) * 0.5;
          return saturate(0.45 + 0.8 * v);
        }

        vec4 main(vec2 p) {
            float fadeIn = subProgress(0.0, 0.13, in_progress);
            float scaleIn = subProgress(0.0, 1.0, in_progress);
            float fadeOutKiraKira = subProgress(0.4, 0.5, in_progress);
            float fadeOutDokiDoki = subProgress(0.4, 1.0, in_progress);
            vec2 center = mix(in_touch, in_origin, saturate(in_progress * 2.0));
            float kiraKiraArea = kiraKiraMask(p, center, in_maxRadius, scaleIn, in_maxRadius * in_kiraKiraWidth, 1.0);
            float alpha = min(fadeIn, 1.0 - fadeOutKiraKira);
            vec2 uv = p * in_resolutionScale;
            vec2 kiraKiraUv = uv - mod(uv, in_kiraKiraScale);
            float mahouStrength = mahou(uv, in_mahouPhase);
            float kiraKiraAlpha = kiraKira(kiraKiraUv, in_kiraKiraPhase) * kiraKiraArea * alpha * mahouStrength;
            float fade = min(fadeIn, 1.0 - fadeOutDokiDoki);
            float dokiDokiAlpha = dokiDoki(p, center, in_maxRadius * scaleIn, 1.0) * fade * in_color.a;
            vec4 dokiDokiColor = vec4(in_color.rgb * dokiDokiAlpha, dokiDokiAlpha);
            vec4 kiraKiraColor = vec4(in_kiraKiraColor.rgb * in_kiraKiraColor.a, in_kiraKiraColor.a);
            return mix(dokiDokiColor, kiraKiraColor, kiraKiraAlpha);
        }
        """.trimIndent()
    )

    fun createBrush(size: Size, touch: Offset, origin: Offset, radius: Float, progress: Float, kiraKiraPhaseMillis: Float, color: Color): ShaderBrush {
        val width = size.width.coerceAtLeast(1f)
        val height = size.height.coerceAtLeast(1f)
        val mahouRotation1 = kiraKiraPhaseMillis * PI_ROTATE_RIGHT + 1.7 * PI
        val mahouRotation2 = kiraKiraPhaseMillis * PI_ROTATE_LEFT + 2.0 * PI
        val mahouRotation3 = kiraKiraPhaseMillis * PI_ROTATE_RIGHT + 2.75 * PI

        val shader = RuntimeShaderBuilder(effect).apply {
            uniform("in_origin", origin.x, origin.y)
            uniform("in_touch", touch.x, touch.y)
            uniform("in_progress", progress)
            uniform("in_maxRadius", radius * DOKI_DOKI_RADIUS_FACTOR)
            uniform("in_resolutionScale", 1f / width, 1f / height)
            uniform("in_kiraKiraScale", KIRA_KIRA_SCALE / width, KIRA_KIRA_SCALE / height)
            uniform("in_kiraKiraPhase", kiraKiraPhaseMillis * 0.001f)
            uniform("in_kiraKiraWidth", KIRA_KIRA_WIDTH)
            uniform("in_kiraKiraStrength", KIRA_KIRA_STRENGTH)
            uniform("in_mahouPhase", kiraKiraPhaseMillis)
            uniform("in_mahouCircle1", MAHOU_SCALE * 0.5f + (kiraKiraPhaseMillis * 0.01f * cos(MAHOU_SCALE * 0.55f)), MAHOU_SCALE * 0.5f + (kiraKiraPhaseMillis * 0.01f * sin(MAHOU_SCALE * 0.55f)))
            uniform("in_mahouCircle2", MAHOU_SCALE * 0.2f + (kiraKiraPhaseMillis * -0.0066f * cos(MAHOU_SCALE * 0.45f)), MAHOU_SCALE * 0.2f + (kiraKiraPhaseMillis * -0.0066f * sin(MAHOU_SCALE * 0.45f)))
            uniform("in_mahouCircle3", MAHOU_SCALE + (kiraKiraPhaseMillis * -0.0066f * cos(MAHOU_SCALE * 0.35f)), MAHOU_SCALE + (kiraKiraPhaseMillis * -0.0066f * sin(MAHOU_SCALE * 0.35f)))
            uniform("in_mahouRotation1", cos(mahouRotation1).toFloat(), sin(mahouRotation1).toFloat())
            uniform("in_mahouRotation2", cos(mahouRotation2).toFloat(), sin(mahouRotation2).toFloat())
            uniform("in_mahouRotation3", cos(mahouRotation3).toFloat(), sin(mahouRotation3).toFloat())
            uniform("in_color", color.red, color.green, color.blue, color.alpha)
            uniform("in_kiraKiraColor", KIRA_KIRA_COLOR.red, KIRA_KIRA_COLOR.green, KIRA_KIRA_COLOR.blue, KIRA_KIRA_COLOR.alpha)
        }.makeShader().asComposeShader()

        return ShaderBrush(shader)
    }
}
