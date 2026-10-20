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

@file:JvmName("AnimatedVectorResources_androidKt")
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package androidx.compose.animation.graphics.res

// TIPS：移植自 AndroidX animation-graphics，加载安卓 animated-vector 资源，与 AndroidX 同名同义

import android.content.res.Resources
import android.util.Xml
import androidx.annotation.DrawableRes
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import me.earzuchan.hiro.compose.internal.res.animated.parseAnimatedImageVector
import me.earzuchan.hiro.compose.internal.res.animated.seekToStartTag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import org.xmlpull.v1.XmlPullParserException

@Composable
public fun AnimatedImageVector.Companion.animatedVectorResource(
    @DrawableRes id: Int
): AnimatedImageVector {
    val context = LocalContext.current
    val res = LocalResources.current
    val theme = context.theme
    return remember(id) { loadAnimatedVectorResource(theme, res, id) }
}

@Throws(XmlPullParserException::class)
internal fun loadAnimatedVectorResource(
    theme: Resources.Theme? = null,
    res: Resources,
    resId: Int,
): AnimatedImageVector {
    val parser = res.getXml(resId).seekToStartTag()
    val attrs = Xml.asAttributeSet(parser)
    return parser.parseAnimatedImageVector(res, theme, attrs)
}
