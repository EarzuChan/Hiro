package me.earzuchan.hiro.compose.internal.windowinsets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets

@OptIn(InternalComposeUiApi::class)
class HiroMutablePlatformWindowInsets : PlatformWindowInsets { // TIPS：AI 建议是不要使得这个类公开化，而是让用户想自定义时传一个快照
    private val captionBarInsets = HiroMutablePlatformInsets()
    private val displayCutoutInsets = HiroMutablePlatformInsets()
    private val imeInsets = HiroMutablePlatformInsets()
    private val mandatorySystemGesturesInsets = HiroMutablePlatformInsets()
    private val navigationBarsInsets = HiroMutablePlatformInsets()
    private val statusBarsInsets = HiroMutablePlatformInsets()
    private val systemBarsInsets = HiroMutablePlatformInsets()
    private val systemGesturesInsets = HiroMutablePlatformInsets()
    private val tappableElementInsets = HiroMutablePlatformInsets()
    private val waterfallInsets = HiroMutablePlatformInsets()
    private val excludedInsetsCache = mutableMapOf<Pair<Boolean, Boolean>, PlatformWindowInsets>()

    private var displayCutoutBounds by mutableStateOf(emptyList<Rect>())

    override val displayCutouts get() = displayCutoutBounds
    override val captionBar: PlatformInsets get() = captionBarInsets
    override val displayCutout: PlatformInsets get() = displayCutoutInsets
    override val ime: PlatformInsets get() = imeInsets
    override val mandatorySystemGestures: PlatformInsets get() = mandatorySystemGesturesInsets
    override val navigationBars: PlatformInsets get() = navigationBarsInsets
    override val statusBars: PlatformInsets get() = statusBarsInsets
    override val systemBars: PlatformInsets get() = systemBarsInsets
    override val systemGestures: PlatformInsets get() = systemGesturesInsets
    override val tappableElement: PlatformInsets get() = tappableElementInsets
    override val waterfall: PlatformInsets get() = waterfallInsets

    // TIPS：这是控制反转，被上级调用以更新值
    fun update(next: HiroPlatformWindowInsetsSnapshot): Boolean {
        var changed = false

        changed = captionBarInsets.update(next.captionBar) || changed
        changed = displayCutoutInsets.update(next.displayCutout) || changed
        changed = imeInsets.update(next.ime) || changed
        changed = mandatorySystemGesturesInsets.update(next.mandatorySystemGestures) || changed
        changed = navigationBarsInsets.update(next.navigationBars) || changed
        changed = statusBarsInsets.update(next.statusBars) || changed
        changed = systemBarsInsets.update(next.systemBars) || changed
        changed = systemGesturesInsets.update(next.systemGestures) || changed
        changed = tappableElementInsets.update(next.tappableElement) || changed
        changed = waterfallInsets.update(next.waterfall) || changed

        if (displayCutoutBounds != next.displayCutouts) {
            displayCutoutBounds = next.displayCutouts
            changed = true
        }

        if (changed) Snapshot.sendApplyNotifications()

        return changed
    }

    override fun excluding(safeInsets: Boolean, ime: Boolean): PlatformWindowInsets {
        if (!safeInsets && !ime) return this

        return excludedInsetsCache.getOrPut(safeInsets to ime) { HiroExcludedPlatformWindowInsets(this, safeInsets, ime) }
    }
}

data class HiroPlatformWindowInsetsSnapshot(
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
)

data class HiroInsetsValues(val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0) {
    fun normalized() = HiroInsetsValues(
        left = left.coerceAtLeast(0),
        top = top.coerceAtLeast(0),
        right = right.coerceAtLeast(0),
        bottom = bottom.coerceAtLeast(0),
    )

    companion object {
        val Zero = HiroInsetsValues()
    }
}

@OptIn(InternalComposeUiApi::class)
private class HiroMutablePlatformInsets : PlatformInsets {
    private var values by mutableStateOf(HiroInsetsValues.Zero)

    override val left get() = values.left
    override val top get() = values.top
    override val right get() = values.right
    override val bottom get() = values.bottom

    fun update(next: HiroInsetsValues): Boolean {
        val normalized = next.normalized()
        if (values == normalized) return false

        values = normalized
        return true
    }
}

@OptIn(InternalComposeUiApi::class)
private class HiroExcludedPlatformWindowInsets(private val source: HiroMutablePlatformWindowInsets, private val excludeSafeInsets: Boolean, private val excludeIme: Boolean) : PlatformWindowInsets {
    override val displayCutouts get() = if (excludeSafeInsets) emptyList() else source.displayCutouts
    override val captionBar get() = source.captionBar.excludeWhen(excludeSafeInsets)
    override val displayCutout get() = source.displayCutout.excludeWhen(excludeSafeInsets)
    override val ime get() = source.ime.excludeWhen(excludeIme)
    override val mandatorySystemGestures get() = source.mandatorySystemGestures.excludeWhen(excludeSafeInsets)
    override val navigationBars get() = source.navigationBars.excludeWhen(excludeSafeInsets)
    override val statusBars get() = source.statusBars.excludeWhen(excludeSafeInsets)
    override val systemBars get() = source.systemBars.excludeWhen(excludeSafeInsets)
    override val systemGestures get() = source.systemGestures.excludeWhen(excludeSafeInsets)
    override val tappableElement get() = source.tappableElement.excludeWhen(excludeSafeInsets)
    override val waterfall get() = source.waterfall.excludeWhen(excludeSafeInsets)
}

@OptIn(InternalComposeUiApi::class)
private fun PlatformInsets.excludeWhen(exclude: Boolean) = if (!exclude) this else PlatformInsets.Zero
