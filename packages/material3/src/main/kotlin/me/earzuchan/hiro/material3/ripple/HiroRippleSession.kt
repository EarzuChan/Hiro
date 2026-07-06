package me.earzuchan.hiro.material3.ripple

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

internal class HiroRippleSession(private var rippleSize: Size, private var touch: Offset, private var targetRadius: Float) {
    private val finishSignal = CompletableDeferred<Unit>()
    private val progress = Animatable(0f)
    private val startedAtMillis = SystemClock.uptimeMillis()
    private var noisePhaseMillis by mutableFloatStateOf(startedAtMillis.toFloat())

    val currentProgress: Float get() = progress.value

    val currentNoisePhaseMillis: Float get() = noisePhaseMillis

    val currentTouch: Offset get() = touch

    val currentOrigin: Offset get() = rippleSize.center

    val currentRadius: Float get() = targetRadius

    fun updateGeometry(neoRippleSize: Size, neoTargetRadius: Float) {
        rippleSize = neoRippleSize
        targetRadius = neoTargetRadius
    }

    fun finish() {
        finishSignal.complete(Unit)
    }

    suspend fun play(invalidate: () -> Unit) = coroutineScope {
        val noiseJob = launch {
            while (isActive) {
                withFrameNanos { frameNanos -> noisePhaseMillis = frameNanos / 1_000_000f }
                invalidate()
            }
        }

        val progressJob = launch {
            progress.animateTo(0.5f, tween(durationMillis = ENTER_DURATION, easing = FastOutSlowInEasing))
            finishSignal.await()
            val delayMillis = max(ENTER_DURATION - (SystemClock.uptimeMillis() - startedAtMillis), 0L)
            if (delayMillis > 0) delay(delayMillis.milliseconds)
            progress.animateTo(1f, tween(durationMillis = EXIT_DURATION, easing = LinearEasing))
        }

        try {
            progressJob.join()
        } finally {
            noiseJob.cancel()
            joinAll(noiseJob)
        }
    }

    companion object {
        const val ENTER_DURATION = 450
        const val EXIT_DURATION = 375
    }
}

private val Size.center: Offset get() = Offset(width / 2f, height / 2f)
