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
    private const val RADIUS_FACTOR = 2.3f
    private const val NOISE_DENSITY_SCALE = 2.1f
    private const val TURBULENCE_SCALE = 1.5f
    private const val PI_ROTATE_RIGHT = PI * 0.0078125
    private const val PI_ROTATE_LEFT = PI * -0.0078125
    private val SPARKLE_COLOR = Color(0x8DFFFFFF)

    private val effect = RuntimeEffect.makeForShader(
        """
        uniform vec2 in_origin;
        uniform vec2 in_touch;
        uniform float in_progress;
        uniform float in_maxRadius;
        uniform vec2 in_resolutionScale;
        uniform vec2 in_noiseScale;
        uniform float in_noisePhase;
        uniform float in_turbulencePhase;
        uniform vec2 in_tCircle1;
        uniform vec2 in_tCircle2;
        uniform vec2 in_tCircle3;
        uniform vec2 in_tRotation1;
        uniform vec2 in_tRotation2;
        uniform vec2 in_tRotation3;
        uniform vec4 in_color;
        uniform vec4 in_sparkleColor;

        float triangleNoise(vec2 n) {
          n  = fract(n * vec2(5.3987, 5.4421));
          n += dot(n.yx, n.xy + vec2(21.5351, 14.3137));
          float xy = n.x * n.y;
          return fract(xy * 95.4307) + fract(xy * 75.04961) - 1.0;
        }

        const float PI = 3.1415926535897932384626;

        float threshold(float v, float l, float h) {
            return step(l, v) * (1.0 - step(h, v));
        }

        float sparkles(vec2 uv, float t) {
          float n = triangleNoise(uv);
          float s = 0.0;
          for (float i = 0.0; i < 4.0; i += 1.0) {
            float l = i * 0.1;
            float h = l + 0.05;
            float o = sin(PI * (t + 0.35 * i));
            s += threshold(n + o, l, h);
          }
          return saturate(s) * in_sparkleColor.a;
        }

        float softCircle(vec2 uv, vec2 xy, float radius, float blur) {
          float blurHalf = blur * 0.5;
          float d = distance(uv, xy);
          return 1.0 - smoothstep(1.0 - blurHalf, 1.0 + blurHalf, d / radius);
        }

        float softRing(vec2 uv, vec2 xy, float radius, float progress, float blur) {
          float thickness = 0.05 * radius;
          float currentRadius = radius * progress;
          float circle_outer = softCircle(uv, xy, currentRadius + thickness, blur);
          float circle_inner = softCircle(uv, xy, max(currentRadius - thickness, 0.0), blur);
          return saturate(circle_outer - circle_inner);
        }

        float subProgress(float start, float end, float progress) {
            float sub = clamp(progress, start, end);
            return (sub - start) / (end - start);
        }

        mat2 rotate2d(vec2 rad) {
          return mat2(rad.x, -rad.y, rad.y, rad.x);
        }

        float circle_grid(vec2 resolution, vec2 coord, float time, vec2 center, vec2 rotation, float cell_diameter) {
          coord = rotate2d(rotation) * (center - coord) + center;
          coord = mod(coord, cell_diameter) / resolution;
          float normal_radius = cell_diameter / resolution.y * 0.5;
          float radius = 0.65 * normal_radius;
          return softCircle(coord, vec2(normal_radius), radius, radius * 50.0);
        }

        float turbulence(vec2 uv, float t) {
          const vec2 scale = vec2(0.8);
          uv = uv * scale;
          float g1 = circle_grid(scale, uv, t, in_tCircle1, in_tRotation1, 0.17);
          float g2 = circle_grid(scale, uv, t, in_tCircle2, in_tRotation2, 0.2);
          float g3 = circle_grid(scale, uv, t, in_tCircle3, in_tRotation3, 0.275);
          float v = (g1 * g1 + g2 - g3) * 0.5;
          return saturate(0.45 + 0.8 * v);
        }

        vec4 main(vec2 p) {
            float fadeIn = subProgress(0.0, 0.13, in_progress);
            float scaleIn = subProgress(0.0, 1.0, in_progress);
            float fadeOutNoise = subProgress(0.4, 0.5, in_progress);
            float fadeOutRipple = subProgress(0.4, 1.0, in_progress);
            vec2 center = mix(in_touch, in_origin, saturate(in_progress * 2.0));
            float ring = softRing(p, center, in_maxRadius, scaleIn, 1.0);
            float alpha = min(fadeIn, 1.0 - fadeOutNoise);
            vec2 uv = p * in_resolutionScale;
            vec2 densityUv = uv - mod(uv, in_noiseScale);
            float turb = turbulence(uv, in_turbulencePhase);
            float sparkleAlpha = sparkles(densityUv, in_noisePhase) * ring * alpha * turb;
            float fade = min(fadeIn, 1.0 - fadeOutRipple);
            float waveAlpha = softCircle(p, center, in_maxRadius * scaleIn, 1.0) * fade * in_color.a;
            vec4 waveColor = vec4(in_color.rgb * waveAlpha, waveAlpha);
            vec4 sparkleColor = vec4(in_sparkleColor.rgb * in_sparkleColor.a, in_sparkleColor.a);
            return mix(waveColor, sparkleColor, sparkleAlpha);
        }
        """.trimIndent()
    )

    fun createBrush(size: Size, touch: Offset, origin: Offset, radius: Float, progress: Float, noisePhaseMillis: Float, color: Color): ShaderBrush {
        val width = size.width.coerceAtLeast(1f)
        val height = size.height.coerceAtLeast(1f)
        val rotation1 = noisePhaseMillis * PI_ROTATE_RIGHT + 1.7 * PI
        val rotation2 = noisePhaseMillis * PI_ROTATE_LEFT + 2.0 * PI
        val rotation3 = noisePhaseMillis * PI_ROTATE_RIGHT + 2.75 * PI

        val shader = RuntimeShaderBuilder(effect).apply {
            uniform("in_origin", origin.x, origin.y)
            uniform("in_touch", touch.x, touch.y)
            uniform("in_progress", progress)
            uniform("in_maxRadius", radius * RADIUS_FACTOR)
            uniform("in_resolutionScale", 1f / width, 1f / height)
            uniform("in_noiseScale", NOISE_DENSITY_SCALE / width, NOISE_DENSITY_SCALE / height)
            uniform("in_noisePhase", noisePhaseMillis * 0.001f)
            uniform("in_turbulencePhase", noisePhaseMillis)
            uniform("in_tCircle1", TURBULENCE_SCALE * 0.5f + (noisePhaseMillis * 0.01f * cos(TURBULENCE_SCALE * 0.55f)), TURBULENCE_SCALE * 0.5f + (noisePhaseMillis * 0.01f * sin(TURBULENCE_SCALE * 0.55f)))
            uniform("in_tCircle2", TURBULENCE_SCALE * 0.2f + (noisePhaseMillis * -0.0066f * cos(TURBULENCE_SCALE * 0.45f)), TURBULENCE_SCALE * 0.2f + (noisePhaseMillis * -0.0066f * sin(TURBULENCE_SCALE * 0.45f)))
            uniform("in_tCircle3", TURBULENCE_SCALE + (noisePhaseMillis * -0.0066f * cos(TURBULENCE_SCALE * 0.35f)), TURBULENCE_SCALE + (noisePhaseMillis * -0.0066f * sin(TURBULENCE_SCALE * 0.35f)))
            uniform("in_tRotation1", cos(rotation1).toFloat(), sin(rotation1).toFloat())
            uniform("in_tRotation2", cos(rotation2).toFloat(), sin(rotation2).toFloat())
            uniform("in_tRotation3", cos(rotation3).toFloat(), sin(rotation3).toFloat())
            uniform("in_color", color.red, color.green, color.blue, color.alpha)
            uniform("in_sparkleColor", SPARKLE_COLOR.red, SPARKLE_COLOR.green, SPARKLE_COLOR.blue, SPARKLE_COLOR.alpha)
        }.makeShader().asComposeShader()

        return ShaderBrush(shader)
    }
}
