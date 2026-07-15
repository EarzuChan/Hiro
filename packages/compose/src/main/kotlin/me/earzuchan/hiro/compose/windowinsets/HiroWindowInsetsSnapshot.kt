package me.earzuchan.hiro.compose.windowinsets

import androidx.compose.ui.geometry.Rect

/** 可跨线程运输并供用户客制化的窗口 Insets 不可变快照 */
data class HiroWindowInsetsSnapshot(
    val captionBar: HiroInsetsValues = HiroInsetsValues.Zero,
    val displayCutout: HiroInsetsValues = HiroInsetsValues.Zero,
    val ime: HiroInsetsValues = HiroInsetsValues.Zero,
    val mandatorySystemGestures: HiroInsetsValues = HiroInsetsValues.Zero,
    val navigationBars: HiroInsetsValues = HiroInsetsValues.Zero,
    val statusBars: HiroInsetsValues = HiroInsetsValues.Zero,
    val systemBars: HiroInsetsValues = HiroInsetsValues.Zero,
    val systemGestures: HiroInsetsValues = HiroInsetsValues.Zero,
    val tappableElement: HiroInsetsValues = HiroInsetsValues.Zero,
    val waterfall: HiroInsetsValues = HiroInsetsValues.Zero,
    val displayCutouts: List<Rect> = emptyList(),
) {
    companion object {
        @JvmField
        val Zero = HiroWindowInsetsSnapshot()
    }
}

/** 以宿主 View 像素为单位的四向 Insets 值 */
data class HiroInsetsValues(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
) {
    companion object {
        @JvmField
        val Zero = HiroInsetsValues()
    }
}

internal fun HiroWindowInsetsSnapshot.normalized() = copy(
    captionBar = captionBar.normalized(),
    displayCutout = displayCutout.normalized(),
    ime = ime.normalized(),
    mandatorySystemGestures = mandatorySystemGestures.normalized(),
    navigationBars = navigationBars.normalized(),
    statusBars = statusBars.normalized(),
    systemBars = systemBars.normalized(),
    systemGestures = systemGestures.normalized(),
    tappableElement = tappableElement.normalized(),
    waterfall = waterfall.normalized(),
    displayCutouts = displayCutouts.toList(),
)

private fun HiroInsetsValues.normalized() = copy(
    left = left.coerceAtLeast(0),
    top = top.coerceAtLeast(0),
    right = right.coerceAtLeast(0),
    bottom = bottom.coerceAtLeast(0),
)
