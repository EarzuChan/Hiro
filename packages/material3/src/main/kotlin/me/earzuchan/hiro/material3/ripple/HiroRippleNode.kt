package me.earzuchan.hiro.material3.ripple

import android.util.Log
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch

class HiroRippleNode(private val interactionSource: InteractionSource, private val bounded: Boolean, private val radius: Dp, private val color: ColorProducer, private val rippleAlpha: () -> RippleAlpha) : Modifier.Node(), CompositionLocalConsumerModifierNode, DrawModifierNode, LayoutAwareModifierNode {
    override val shouldAutoInvalidate: Boolean = false

    private val ripples = linkedMapOf<PressInteraction.Press, HiroRippleSession>()
    private var stateLayer: HiroRippleStateLayer? = null
    private var hasValidSize = false
    private var targetRadius = 0f
    private var rippleSize = Size.Zero
    private val pendingInteractions = mutableListOf<PressInteraction>()

    companion object {
        private const val TAG = "HiroRippleNode"
    }

    override fun onAttach() {
        Log.d(TAG, "将贴附")

        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction -> handlePressInteractionWhenReady(interaction)

                    else -> updateStateLayer(interaction, this)
                }
            }
        }
    }

    override fun onRemeasured(size: IntSize) {
        val nextRippleSize = size.toSize()
        if (rippleSize != nextRippleSize) Log.d(TAG, "节点尺寸改变：${nextRippleSize.width}x${nextRippleSize.height}")

        hasValidSize = true
        rippleSize = nextRippleSize
        targetRadius = with(requireDensity()) { if (radius.isUnspecified) getHiroRippleEndRadius(bounded, rippleSize) else radius.toPx() }
        ripples.values.forEach { session -> session.updateGeometry(rippleSize, targetRadius) }
        pendingInteractions.forEach(::handlePressInteraction)
        pendingInteractions.clear()
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        stateLayer?.run { drawStateLayer(targetRadius, rippleColor) }
        drawRipples()
    }

    override fun onDetach() {
        Log.d(TAG, "将脱离")

        ripples.values.forEach { session -> session.finish() }
        ripples.clear()
    }

    private val rippleColor: Color get() = color()

    private fun handlePressInteractionWhenReady(interaction: PressInteraction) {
        if (hasValidSize) handlePressInteraction(interaction) else pendingInteractions.add(interaction)
    }

    private fun handlePressInteraction(interaction: PressInteraction) {
        when (interaction) {
            is PressInteraction.Press -> addRipple(interaction)
            is PressInteraction.Release -> removeRipple(interaction.press)
            is PressInteraction.Cancel -> removeRipple(interaction.press)
        }
    }

    private fun addRipple(interaction: PressInteraction.Press) {
        Log.d(TAG, "将添加波纹：${interaction.pressPosition}")

        ripples.values.forEach { session -> session.finish() }

        val touch = if (bounded) interaction.pressPosition else rippleSize.center
        val session = HiroRippleSession(rippleSize = rippleSize, touch = touch, targetRadius = targetRadius)
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

    private fun removeRipple(interaction: PressInteraction.Press) {
        Log.d(TAG, "将移除波纹")

        ripples[interaction]?.finish()
    }

    private fun updateStateLayer(interaction: Interaction, scope: kotlinx.coroutines.CoroutineScope) {
        val layer = stateLayer ?: HiroRippleStateLayer(
            bounded = bounded,
            draggedAlpha = { rippleAlpha().draggedAlpha },
            focusedAlpha = { rippleAlpha().focusedAlpha },
            hoveredAlpha = { rippleAlpha().hoveredAlpha },
        ).also { stateLayer = it }

        layer.handleInteraction(interaction, scope)
        invalidateDraw()
    }

    private fun DrawScope.drawRipples() {
        val alpha = rippleAlpha().pressedAlpha
        if (alpha <= 0f || targetRadius <= 0f || rippleSize == Size.Zero) return

        val modulatedColor = rippleColor.copy(alpha = rippleColor.alpha * alpha)
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
}

private val Size.center: Offset get() = Offset(width / 2f, height / 2f)
