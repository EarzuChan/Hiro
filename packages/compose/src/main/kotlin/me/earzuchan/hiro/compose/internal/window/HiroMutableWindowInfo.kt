package me.earzuchan.hiro.compose.internal.window

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize

internal class HiroMutableWindowInfo : WindowInfo {
    override val isWindowFocused get() = true

    override var containerSize by mutableStateOf(IntSize.Zero)
        private set

    override var containerDpSize by mutableStateOf(DpSize.Zero)
        private set

    fun updateContainerSize(size: IntSize, density: Density): Boolean {
        val sizeInDp = with(density) { DpSize(size.width.toDp(), size.height.toDp()) }
        if (containerSize == size && containerDpSize == sizeInDp) return false

        containerSize = size
        containerDpSize = sizeInDp
        return true
    }
}
