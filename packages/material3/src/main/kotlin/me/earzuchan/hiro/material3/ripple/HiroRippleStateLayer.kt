package me.earzuchan.hiro.material3.ripple

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class HiroRippleStateLayer(private val bounded: Boolean, private val draggedAlpha: () -> Float, private val focusedAlpha: () -> Float, private val hoveredAlpha: () -> Float) {
    private val animatedAlpha = Animatable(0f)
    private val interactions = mutableListOf<Interaction>()
    private var currentInteraction: Interaction? = null

    fun handleInteraction(interaction: Interaction, scope: CoroutineScope) {
        when (interaction) {
            is HoverInteraction.Enter -> interactions.add(interaction)
            is HoverInteraction.Exit -> interactions.remove(interaction.enter)
            is FocusInteraction.Focus -> interactions.add(interaction)
            is FocusInteraction.Unfocus -> interactions.remove(interaction.focus)
            is DragInteraction.Start -> interactions.add(interaction)
            is DragInteraction.Stop -> interactions.remove(interaction.start)
            is DragInteraction.Cancel -> interactions.remove(interaction.start)
            else -> return
        }

        val newInteraction = interactions.lastOrNull()
        if (currentInteraction == newInteraction) return

        if (newInteraction != null) {
            val targetAlpha = when (newInteraction) {
                is HoverInteraction.Enter -> hoveredAlpha()
                is FocusInteraction.Focus -> focusedAlpha()
                is DragInteraction.Start -> draggedAlpha()
                else -> 0f
            }

            scope.launch { animatedAlpha.animateTo(targetAlpha, incomingStateLayerAnimationSpecFor(newInteraction)) }
        } else scope.launch { animatedAlpha.animateTo(0f, outgoingStateLayerAnimationSpecFor(currentInteraction)) }

        currentInteraction = newInteraction
    }

    fun DrawScope.drawStateLayer(radius: Float, color: Color) {
        val alpha = animatedAlpha.value
        if (alpha <= 0f) return

        val modulatedColor = color.copy(alpha = alpha)
        if (bounded) clipRect { drawCircle(modulatedColor, radius) } // 额外又Clip一次，有感觉吗
        else drawCircle(modulatedColor, radius)
    }
}

private fun incomingStateLayerAnimationSpecFor(interaction: Interaction) = when (interaction) {
    is HoverInteraction.Enter -> DefaultTweenSpec
    is FocusInteraction.Focus -> TweenSpec(durationMillis = 45, easing = LinearEasing)
    is DragInteraction.Start -> TweenSpec(durationMillis = 45, easing = LinearEasing)
    else -> DefaultTweenSpec
}

private fun outgoingStateLayerAnimationSpecFor(interaction: Interaction?) = when (interaction) {
    is HoverInteraction.Enter -> DefaultTweenSpec
    is FocusInteraction.Focus -> DefaultTweenSpec
    is DragInteraction.Start -> TweenSpec(durationMillis = 150, easing = LinearEasing)
    else -> DefaultTweenSpec
}

private val DefaultTweenSpec = TweenSpec<Float>(durationMillis = 15, easing = LinearEasing)
