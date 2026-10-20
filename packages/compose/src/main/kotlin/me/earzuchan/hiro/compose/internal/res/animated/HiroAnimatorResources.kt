/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package me.earzuchan.hiro.compose.internal.res.animated

// TIPS：移植自 AndroidX animation-graphics，安卓 Animator 与插值器资源的解析

import android.animation.TimeInterpolator
import android.content.res.Resources
import android.util.Xml
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.BounceInterpolator
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.graphics.vector.Animator
import me.earzuchan.hiro.compose.internal.res.animated.AndroidVectorResources
import me.earzuchan.hiro.compose.internal.res.animated.TagObjectAnimator
import me.earzuchan.hiro.compose.internal.res.animated.TagSet
import me.earzuchan.hiro.compose.internal.res.animated.parseAnimatorSet
import me.earzuchan.hiro.compose.internal.res.animated.parseInterpolator
import me.earzuchan.hiro.compose.internal.res.animated.parseObjectAnimator
import me.earzuchan.hiro.compose.internal.res.animated.seekToStartTag
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import org.xmlpull.v1.XmlPullParserException

@Throws(XmlPullParserException::class)
internal fun loadAnimatorResource(
    theme: Resources.Theme? = null,
    res: Resources,
    resId: Int,
): Animator {
    val parser = res.getXml(resId)
    val attrs = Xml.asAttributeSet(parser)

    parser.seekToStartTag()
    return when (parser.name) {
        TagSet -> {
            parser.parseAnimatorSet(res, theme, attrs)
        }
        TagObjectAnimator -> {
            parser.parseObjectAnimator(res, theme, attrs)
        }
        else -> {
            throw XmlPullParserException("Unknown tag: ${parser.name}")
        }
    }
}

internal fun TimeInterpolator.toEasing() = Easing { x -> getInterpolation(x) }

internal val AccelerateDecelerateEasing = Easing { x ->
    ((cos((x + 1) * Math.PI) / 2.0f) + 0.5f).toFloat()
}

internal val AccelerateEasing = Easing { x -> x * x }

internal fun AccelerateEasing(factor: Float) = Easing { x -> x.pow(factor * 2) }

internal fun AnticipateEasing(tension: Float) = Easing { x ->
    x * x * ((tension + 1) * x - tension)
}

internal fun AnticipateOvershootEasing(tension: Float, extraTension: Float): Easing =
    AnticipateOvershootInterpolator(tension, extraTension).toEasing()

internal val BounceEasing: Easing = BounceInterpolator().toEasing()

internal fun CycleEasing(cycle: Float) = Easing { x -> sin(2 * cycle * PI * x).toFloat() }

internal val DecelerateEasing = Easing { x -> 1.0f - (1.0f - x) * (1.0f - x) }

internal fun DecelerateEasing(factor: Float) = Easing { x -> 1.0f - (1.0f - x).pow(2 * factor) }

internal fun OvershootEasing(tension: Float) = Easing { x ->
    (x - 1f).let { t -> t * t * ((tension + 1f) * t + tension) + 1f }
}

private val builtinInterpolators =
    hashMapOf(
        android.R.anim.linear_interpolator to LinearEasing,
        android.R.interpolator.fast_out_linear_in to FastOutLinearInEasing,
        android.R.interpolator.fast_out_slow_in to FastOutSlowInEasing,
        android.R.interpolator.linear to LinearEasing,
        android.R.interpolator.linear_out_slow_in to LinearOutSlowInEasing,
        AndroidVectorResources.FAST_OUT_LINEAR_IN to FastOutLinearInEasing,
        AndroidVectorResources.FAST_OUT_SLOW_IN to FastOutSlowInEasing,
        AndroidVectorResources.LINEAR_OUT_SLOW_IN to LinearOutSlowInEasing,
    )

@Throws(XmlPullParserException::class)
internal fun loadInterpolatorResource(
    theme: Resources.Theme? = null,
    res: Resources,
    resId: Int,
): Easing {
    return builtinInterpolators[resId]
        ?: res.getXml(resId).run {
            seekToStartTag().parseInterpolator(res, theme, Xml.asAttributeSet(this))
        }
}
