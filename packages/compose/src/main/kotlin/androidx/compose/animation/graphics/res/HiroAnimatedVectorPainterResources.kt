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

@file:JvmName("AnimatedVectorPainterResources_androidKt")
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package androidx.compose.animation.graphics.res

// TIPS：移植自 AndroidX animation-graphics，动画矢量 Painter，与 AndroidX 同名同义

import androidx.annotation.VisibleForTesting
import androidx.collection.mutableScatterMapOf
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.graphics.vector.StateVectorConfig
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.RenderVectorGroup
import androidx.compose.ui.graphics.vector.VectorComposable
import androidx.compose.ui.graphics.vector.VectorConfig
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.util.fastForEach

@Composable
public fun rememberAnimatedVectorPainter(
    animatedImageVector: AnimatedImageVector,
    atEnd: Boolean,
): Painter {
    return rememberAnimatedVectorPainter(animatedImageVector, atEnd) { group, overrides ->
        RenderVectorGroup(group, overrides)
    }
}

@VisibleForTesting
@Composable
internal fun rememberAnimatedVectorPainter(
    animatedImageVector: AnimatedImageVector,
    atEnd: Boolean,
    render: @Composable @VectorComposable (VectorGroup, Map<String, VectorConfig>) -> Unit,
): Painter {
    return rememberVectorPainter(
        defaultWidth = animatedImageVector.imageVector.defaultWidth,
        defaultHeight = animatedImageVector.imageVector.defaultHeight,
        viewportWidth = animatedImageVector.imageVector.viewportWidth,
        viewportHeight = animatedImageVector.imageVector.viewportHeight,
        name = animatedImageVector.imageVector.name,
        tintColor = animatedImageVector.imageVector.tintColor,
        tintBlendMode = animatedImageVector.imageVector.tintBlendMode,
        autoMirror = true,
    ) { _, _ ->
        val transition = updateTransition(atEnd, label = animatedImageVector.imageVector.name)
        val map = mutableScatterMapOf<String, StateVectorConfig>()
        animatedImageVector.targets.fastForEach { target ->
            val config =
                target.animator.createVectorConfig(transition, animatedImageVector.totalDuration)
            val currentConfig = map[target.name]
            if (currentConfig != null) {
                currentConfig.merge(config)
            } else {
                map[target.name] = config
            }
        }
        @Suppress("AsCollectionCall") render(animatedImageVector.imageVector.root, map.asMap())
    }
}
