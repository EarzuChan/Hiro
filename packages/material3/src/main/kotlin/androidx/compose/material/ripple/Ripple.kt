package androidx.compose.material.ripple

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.unit.Dp
import me.earzuchan.hiro.material3.ripple.HiroRippleNode

fun createRippleModifierNode(interactionSource: InteractionSource, bounded: Boolean, radius: Dp, color: ColorProducer, rippleAlpha: () -> RippleAlpha): DelegatableNode = HiroRippleNode(interactionSource = interactionSource, bounded = bounded, radius = radius, color = color, rippleAlpha = rippleAlpha)
