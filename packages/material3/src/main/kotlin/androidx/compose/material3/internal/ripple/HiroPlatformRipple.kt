@file:JvmName("Ripple_skikoKt")
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package androidx.compose.material3.internal.ripple

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.unit.Dp
import me.earzuchan.hiro.material3.ripple.HiroRippleNode

internal fun createPlatformRippleNode(interactionSource: InteractionSource, bounded: Boolean, radius: Dp, color: ColorProducer, rippleNodeConfig: () -> RippleNodeConfig): DelegatableNode = HiroRippleNode(interactionSource, bounded, radius, color, rippleNodeConfig)
