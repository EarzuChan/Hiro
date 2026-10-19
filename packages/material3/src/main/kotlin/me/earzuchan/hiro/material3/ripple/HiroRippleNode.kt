@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package me.earzuchan.hiro.material3.ripple

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.internal.ripple.RippleNode
import androidx.compose.material3.internal.ripple.RippleNodeConfig
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import me.earzuchan.hiro.log.HiroLog

// TIPS：悬浮、焦点、拖动的状态层与焦点环由上游 RippleNode 统一处理，Hiro 只接管按压波纹的着色器绘制
internal class HiroRippleNode(interactionSource: InteractionSource, bounded: Boolean, radius: Dp, color: ColorProducer, rippleNodeConfig: () -> RippleNodeConfig) : RippleNode(interactionSource, bounded, radius, color, rippleNodeConfig) {
    private val ripples = linkedMapOf<PressInteraction.Press, HiroRippleSession>()

    companion object {
        private const val TAG = "HiroRippleNode"
    }

    override fun onRemeasured(size: IntSize) {
        super.onRemeasured(size)

        ripples.values.forEach { session -> session.updateGeometry(rippleSize, targetRadius) }
    }

    override fun addRipple(interaction: PressInteraction.Press, size: Size, targetRadius: Float) {
        HiroLog.v(TAG) { "将添加波纹：${interaction.pressPosition}，尺寸=${size.width}x${size.height}，半径=$targetRadius，当前数=${ripples.size}" }

        ripples.values.forEach { session -> session.finish() }

        val touch = if (bounded) interaction.pressPosition else size.center
        val session = HiroRippleSession(rippleSize = size, touch = touch, targetRadius = targetRadius)
        ripples[interaction] = session

        coroutineScope.launch {
            try {
                session.play { invalidateDraw() }
            } finally {
                ripples.remove(interaction)
                invalidateDraw()
            }
        }

        invalidateDraw()
    }

    override fun removeRipple(interaction: PressInteraction.Press) {
        ripples[interaction]?.finish()
    }

    override fun DrawScope.drawRipples() {
        val alpha = when (val press = rippleNodeConfig().press) {
            is RippleNodeConfig.Press.Opacity -> press.alpha

            else -> 0f
        }
        if (alpha <= 0f || targetRadius <= 0f || rippleSize == Size.Zero || ripples.isEmpty()) return

        val color = rippleColor
        val modulatedColor = color.copy(alpha = color.alpha * alpha)
        val drawBlock: DrawScope.() -> Unit = {
            ripples.values.forEach { session ->
                val brush = HiroRippleShader.createBrush(
                    size = size,
                    touch = session.currentTouch,
                    origin = session.currentOrigin,
                    radius = session.currentRadius,
                    progress = session.currentProgress,
                    kiraKiraPhaseMillis = session.currentKiraKiraPhaseMillis,
                    color = modulatedColor,
                )
                drawRect(brush = brush, size = size)
            }
        }

        if (bounded) clipRect(block = drawBlock) else drawBlock()
    }

    override fun onDetach() {
        super.onDetach()

        ripples.values.forEach { session -> session.finish() }
        ripples.clear()
    }
}

private val Size.center: Offset get() = Offset(width / 2f, height / 2f)
