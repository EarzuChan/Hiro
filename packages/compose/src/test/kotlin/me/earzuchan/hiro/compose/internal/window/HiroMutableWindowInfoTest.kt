package me.earzuchan.hiro.compose.internal.window

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HiroMutableWindowInfoTest {
    @Test
    fun updatesPixelAndDpSizesTogether() {
        val windowInfo = HiroMutableWindowInfo()
        val size = IntSize(1080, 2400)

        assertTrue(windowInfo.updateContainerSize(size, Density(3f)))
        assertEquals(size, windowInfo.containerSize)
        assertEquals(DpSize(360.dp, 800.dp), windowInfo.containerDpSize)
        assertFalse(windowInfo.updateContainerSize(size, Density(3f)))

        assertTrue(windowInfo.updateContainerSize(size, Density(2f)))
        assertEquals(DpSize(540.dp, 1200.dp), windowInfo.containerDpSize)
    }
}
