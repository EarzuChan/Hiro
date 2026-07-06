package me.earzuchan.hiro.material3.ripple

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

internal fun Density.getHiroRippleEndRadius(bounded: Boolean, size: Size): Float {
    val boundsCoverRadius = Offset(size.width, size.height).getDistance() / 2f
    return if (bounded) boundsCoverRadius + HiroBoundedRippleExtraRadius.toPx() else boundsCoverRadius
}

private val HiroBoundedRippleExtraRadius = 10.dp
