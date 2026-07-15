package me.earzuchan.hiro.compose.interaction

import android.content.Context
import android.os.Build
import android.view.ViewConfiguration as AndroidViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import me.earzuchan.hiro.compose.HiroComposeConfigurationDsl

/** 可跨线程运输的 Hiro 交互手感完整调校 */
data class HiroInteractionTuning(
    val longPressTimeoutMillis: Long,
    val doubleTapTimeoutMillis: Long,
    val doubleTapMinTimeMillis: Long,
    val touchSlop: Float,
    val handwritingSlop: Float,
    val minimumTouchTargetSize: DpSize,
    val maximumFlingVelocity: Float,
    val minimumFlingVelocity: Float,
    val handwritingGestureLineMargin: Float,
) {
    init {
        require(longPressTimeoutMillis >= 0L) { "长按超时不能为负数" }
        require(doubleTapTimeoutMillis >= 0L) { "双击超时不能为负数" }
        require(doubleTapMinTimeMillis in 0L..doubleTapTimeoutMillis) { "双击最短间隔必须位于零至双击超时之间" }
        require(touchSlop.isValidDistance()) { "触摸阈值必须是非负有限值" }
        require(handwritingSlop.isValidDistance()) { "手写阈值必须是非负有限值" }
        require(handwritingGestureLineMargin.isValidDistance()) { "手写行边距必须是非负有限值" }
        require(minimumTouchTargetSize.width.value.isValidDistance() && minimumTouchTargetSize.height.value.isValidDistance()) { "最小触摸目标尺寸必须是非负有限值" }
        require(maximumFlingVelocity.isValidDistance()) { "最大 Fling 速度必须是非负有限值" }
        require(minimumFlingVelocity.isValidDistance()) { "最小 Fling 速度必须是非负有限值" }
        require(minimumFlingVelocity <= maximumFlingVelocity) { "最小 Fling 速度不能大于最大 Fling 速度" }
    }
}

/** 为 Compose 或 Android 交互基线覆写少数字段 */
@HiroComposeConfigurationDsl
class HiroInteractionTuningPatchBuilder internal constructor() {
    var longPressTimeoutMillis: Long? = null
    var doubleTapTimeoutMillis: Long? = null
    var doubleTapMinTimeMillis: Long? = null
    var touchSlop: Dp? = null
    var handwritingSlop: Dp? = null
    var minimumTouchTargetSize: DpSize? = null
    var maximumFlingVelocity: Float? = null
    var minimumFlingVelocity: Float? = null
    var handwritingGestureLineMargin: Dp? = null

    internal fun compile() = HiroInteractionTuningPatch(
        longPressTimeoutMillis = longPressTimeoutMillis,
        doubleTapTimeoutMillis = doubleTapTimeoutMillis,
        doubleTapMinTimeMillis = doubleTapMinTimeMillis,
        touchSlop = touchSlop,
        handwritingSlop = handwritingSlop,
        minimumTouchTargetSize = minimumTouchTargetSize,
        maximumFlingVelocity = maximumFlingVelocity,
        minimumFlingVelocity = minimumFlingVelocity,
        handwritingGestureLineMargin = handwritingGestureLineMargin,
    )
}

/** 选择 Hiro 交互手感的来源和变换方式 */
@HiroComposeConfigurationDsl
class HiroInteractionPolicyBuilder internal constructor(initial: HiroInteractionPolicy) {
    private var policy = initial

    fun followSystem(patch: HiroInteractionTuningPatchBuilder.() -> Unit = {}) {
        policy = HiroInteractionPolicy.FollowSystem(HiroInteractionTuningPatchBuilder().apply(patch).compile())
    }

    fun followCompose(patch: HiroInteractionTuningPatchBuilder.() -> Unit = {}) {
        policy = HiroInteractionPolicy.FollowCompose(HiroInteractionTuningPatchBuilder().apply(patch).compile())
    }

    fun fixed(tuning: HiroInteractionTuning) {
        policy = HiroInteractionPolicy.Fixed(tuning)
    }

    fun transformSystem(transformer: (HiroInteractionTuning) -> HiroInteractionTuning) {
        policy = HiroInteractionPolicy.TransformSystem(transformer)
    }

    internal fun build() = policy
}

internal sealed interface HiroInteractionPolicy {
    fun resolve(context: Context, density: Density): HiroInteractionTuning

    class FollowSystem(private val patch: HiroInteractionTuningPatch = HiroInteractionTuningPatch.Empty) : HiroInteractionPolicy {
        override fun resolve(context: Context, density: Density) = patch.applyTo(androidInteractionTuning(context, density), density)
    }

    class FollowCompose(private val patch: HiroInteractionTuningPatch = HiroInteractionTuningPatch.Empty) : HiroInteractionPolicy {
        override fun resolve(context: Context, density: Density) = patch.applyTo(composeInteractionTuning(density), density)
    }

    class Fixed(private val tuning: HiroInteractionTuning) : HiroInteractionPolicy {
        override fun resolve(context: Context, density: Density) = tuning
    }

    class TransformSystem(private val transformer: (HiroInteractionTuning) -> HiroInteractionTuning) : HiroInteractionPolicy {
        override fun resolve(context: Context, density: Density) = transformer(androidInteractionTuning(context, density)).validated()
    }
}

internal data class HiroInteractionTuningPatch(
    val longPressTimeoutMillis: Long?,
    val doubleTapTimeoutMillis: Long?,
    val doubleTapMinTimeMillis: Long?,
    val touchSlop: Dp?,
    val handwritingSlop: Dp?,
    val minimumTouchTargetSize: DpSize?,
    val maximumFlingVelocity: Float?,
    val minimumFlingVelocity: Float?,
    val handwritingGestureLineMargin: Dp?,
) {
    fun applyTo(base: HiroInteractionTuning, density: Density) = base.copy(
        longPressTimeoutMillis = longPressTimeoutMillis ?: base.longPressTimeoutMillis,
        doubleTapTimeoutMillis = doubleTapTimeoutMillis ?: base.doubleTapTimeoutMillis,
        doubleTapMinTimeMillis = doubleTapMinTimeMillis ?: base.doubleTapMinTimeMillis,
        touchSlop = touchSlop?.let { with(density) { it.toPx() } } ?: base.touchSlop,
        handwritingSlop = handwritingSlop?.let { with(density) { it.toPx() } } ?: base.handwritingSlop,
        minimumTouchTargetSize = minimumTouchTargetSize ?: base.minimumTouchTargetSize,
        maximumFlingVelocity = maximumFlingVelocity ?: base.maximumFlingVelocity,
        minimumFlingVelocity = minimumFlingVelocity ?: base.minimumFlingVelocity,
        handwritingGestureLineMargin = handwritingGestureLineMargin?.let { with(density) { it.toPx() } } ?: base.handwritingGestureLineMargin,
    )

    companion object {
        val Empty = HiroInteractionTuningPatch(null, null, null, null, null, null, null, null, null)
    }
}

internal fun composeInteractionTuning(density: Density) = HiroInteractionTuning(
    longPressTimeoutMillis = 500L,
    doubleTapTimeoutMillis = 300L,
    doubleTapMinTimeMillis = 40L,
    touchSlop = with(density) { 18.dp.toPx() },
    handwritingSlop = 2f,
    minimumTouchTargetSize = DpSize(48.dp, 48.dp),
    maximumFlingVelocity = Float.MAX_VALUE,
    minimumFlingVelocity = 0f,
    handwritingGestureLineMargin = 16f,
)

private fun androidInteractionTuning(context: Context, density: Density): HiroInteractionTuning {
    val compose = composeInteractionTuning(density)
    val android = AndroidViewConfiguration.get(context)

    return compose.copy(
        longPressTimeoutMillis = AndroidViewConfiguration.getLongPressTimeout().toLong(),
        doubleTapTimeoutMillis = AndroidViewConfiguration.getDoubleTapTimeout().toLong(),
        touchSlop = android.scaledTouchSlop.toFloat(),
        handwritingSlop = if (Build.VERSION.SDK_INT >= 34) android.scaledHandwritingSlop.toFloat() else compose.handwritingSlop,
        maximumFlingVelocity = android.scaledMaximumFlingVelocity.toFloat(),
        minimumFlingVelocity = android.scaledMinimumFlingVelocity.toFloat(),
        handwritingGestureLineMargin = if (Build.VERSION.SDK_INT >= 34) android.scaledHandwritingGestureLineMargin.toFloat() else compose.handwritingGestureLineMargin,
    )
}

private fun HiroInteractionTuning.validated() = copy()

private fun Float.isValidDistance() = isFinite() && this >= 0f
