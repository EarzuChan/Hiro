/*
 * Copyright 2019 The Android Open Source Project
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

// TIPS：与 AndroidX 原版一致，使用 androidx.core 中限定库组使用的 TypedArrayUtils
@file:Suppress("RestrictedApi")

package me.earzuchan.hiro.compose.internal.res.vector

// TIPS：移植自 AndroidX compose ui 的 XmlVectorParser，复合颜色改为直接产出 Compose Brush 以适配 Skia

import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.DefaultPivotX
import androidx.compose.ui.graphics.vector.DefaultPivotY
import androidx.compose.ui.graphics.vector.DefaultRotation
import androidx.compose.ui.graphics.vector.DefaultScaleX
import androidx.compose.ui.graphics.vector.DefaultScaleY
import androidx.compose.ui.graphics.vector.DefaultTranslationX
import androidx.compose.ui.graphics.vector.DefaultTranslationY
import androidx.compose.ui.graphics.vector.EmptyPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import androidx.core.content.res.TypedArrayUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

private const val LINECAP_BUTT = 0
private const val LINECAP_ROUND = 1
private const val LINECAP_SQUARE = 2

private const val LINEJOIN_MITER = 0
private const val LINEJOIN_ROUND = 1
private const val LINEJOIN_BEVEL = 2

private val FILL_TYPE_WINDING = 0

private const val SHAPE_CLIP_PATH = "clip-path"
private const val SHAPE_GROUP = "group"
private const val SHAPE_PATH = "path"

private fun getStrokeLineCap(id: Int, defValue: StrokeCap = StrokeCap.Butt): StrokeCap =
    when (id) {
        LINECAP_BUTT -> StrokeCap.Butt
        LINECAP_ROUND -> StrokeCap.Round
        LINECAP_SQUARE -> StrokeCap.Square
        else -> defValue
    }

private fun getStrokeLineJoin(id: Int, defValue: StrokeJoin = StrokeJoin.Miter): StrokeJoin =
    when (id) {
        LINEJOIN_MITER -> StrokeJoin.Miter
        LINEJOIN_ROUND -> StrokeJoin.Round
        LINEJOIN_BEVEL -> StrokeJoin.Bevel
        else -> defValue
    }

internal fun XmlPullParser.isAtEnd(): Boolean =
    eventType == XmlPullParser.END_DOCUMENT || (depth < 1 && eventType == XmlPullParser.END_TAG)

internal fun AndroidVectorParser.parseCurrentVectorNode(
    res: Resources,
    attrs: AttributeSet,
    theme: Resources.Theme? = null,
    builder: ImageVector.Builder,
    nestedGroups: Int,
): Int {
    when (xmlParser.eventType) {
        XmlPullParser.START_TAG -> {
            when (xmlParser.name) {
                SHAPE_PATH -> {
                    parsePath(res, theme, attrs, builder)
                }
                SHAPE_CLIP_PATH -> {
                    parseClipPath(res, theme, attrs, builder)
                    return nestedGroups + 1
                }
                SHAPE_GROUP -> {
                    parseGroup(res, theme, attrs, builder)
                    pushGroup(nestedGroups)
                    return 0
                }
            }
        }
        XmlPullParser.END_TAG -> {
            if (SHAPE_GROUP == xmlParser.name) {
                repeat(nestedGroups + 1) { builder.clearGroup() }
                return popGroup()
            }
        }
    }
    return nestedGroups
}

@Throws(XmlPullParserException::class)
internal fun XmlPullParser.seekToStartTag(): XmlPullParser {
    var type = next()
    while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
        type = next()
    }
    if (type != XmlPullParser.START_TAG) {
        throw XmlPullParserException("No start tag found")
    }
    return this
}

internal fun AndroidVectorParser.createVectorImageBuilder(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet,
): ImageVector.Builder {
    val vectorAttrs =
        obtainAttributes(
            res,
            theme,
            attrs,
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_TYPE_ARRAY,
        )

    val autoMirror =
        getNamedBoolean(
            vectorAttrs,
            "autoMirrored",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_AUTO_MIRRORED,
            false,
        )

    val viewportWidth =
        getNamedFloat(
            vectorAttrs,
            "viewportWidth",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_VIEWPORT_WIDTH,
            0.0f,
        )

    val viewportHeight =
        getNamedFloat(
            vectorAttrs,
            "viewportHeight",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_VIEWPORT_HEIGHT,
            0.0f,
        )

    if (viewportWidth <= 0) {
        throw XmlPullParserException(
            vectorAttrs.positionDescription + "<VectorGraphic> tag requires viewportWidth > 0"
        )
    } else if (viewportHeight <= 0) {
        throw XmlPullParserException(
            vectorAttrs.positionDescription + "<VectorGraphic> tag requires viewportHeight > 0"
        )
    }

    val defaultWidth =
        getDimension(vectorAttrs, AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_WIDTH, 0.0f)
    val defaultHeight =
        getDimension(vectorAttrs, AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_HEIGHT, 0.0f)

    val tintColor =
        if (vectorAttrs.hasValue(AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_TINT)) {
            val value = TypedValue()
            vectorAttrs.getValue(AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_TINT, value)
            if (value.type == TypedValue.TYPE_ATTRIBUTE) {
                Color.Unspecified
            } else {
                val tintColorStateList =
                    getNamedColorStateList(
                        vectorAttrs,
                        theme,
                        "tint",
                        AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_TINT,
                    )
                if (tintColorStateList != null) {
                    Color(tintColorStateList.defaultColor)
                } else {
                    Color.Unspecified
                }
            }
        } else {
            Color.Unspecified
        }

    val blendModeValue =
        getInt(vectorAttrs, AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_TINT_MODE, -1)
    val tintBlendMode =
        if (blendModeValue != -1) {
            when (blendModeValue) {
                3 -> BlendMode.SrcOver
                5 -> BlendMode.SrcIn
                9 -> BlendMode.SrcAtop
                14 -> BlendMode.Modulate
                15 -> BlendMode.Screen
                16 -> BlendMode.Plus
                else -> BlendMode.SrcIn
            }
        } else {
            BlendMode.SrcIn
        }

    val defaultWidthDp = (defaultWidth / res.displayMetrics.density).dp
    val defaultHeightDp = (defaultHeight / res.displayMetrics.density).dp

    vectorAttrs.recycle()

    return ImageVector.Builder(
        defaultWidth = defaultWidthDp,
        defaultHeight = defaultHeightDp,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        tintColor = tintColor,
        tintBlendMode = tintBlendMode,
        autoMirror = autoMirror,
    )
}

@Throws(IllegalArgumentException::class)
internal fun AndroidVectorParser.parsePath(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet,
    builder: ImageVector.Builder,
) {
    val a =
        obtainAttributes(res, theme, attrs, AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH)

    val hasPathData = TypedArrayUtils.hasAttribute(xmlParser, "pathData")
    if (!hasPathData) {
        throw IllegalArgumentException("No path data available")
    }

    val name: String =
        getString(a, AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_NAME) ?: ""

    val pathStr = getString(a, AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_PATH_DATA)
    val pathData: List<PathNode> =
        if (pathStr == null) {
            EmptyPath
        } else {
            pathParser.pathStringToNodes(pathStr)
        }

    val fillBrush = getNamedBrush(a, res, theme, "fillColor", AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_FILL_COLOR)
    val fillAlpha =
        getNamedFloat(
            a,
            "fillAlpha",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_FILL_ALPHA,
            1.0f,
        )
    val lineCap =
        getNamedInt(
            a,
            "strokeLineCap",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_LINE_CAP,
            -1,
        )
    val strokeLineCap = getStrokeLineCap(lineCap, StrokeCap.Butt)
    val lineJoin =
        getNamedInt(
            a,
            "strokeLineJoin",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_LINE_JOIN,
            -1,
        )
    val strokeLineJoin = getStrokeLineJoin(lineJoin, StrokeJoin.Miter)
    val strokeMiterLimit =
        getNamedFloat(
            a,
            "strokeMiterLimit",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_MITER_LIMIT,
            4.0f,
        )
    val strokeBrush = getNamedBrush(a, res, theme, "strokeColor", AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_COLOR)
    val strokeAlpha =
        getNamedFloat(
            a,
            "strokeAlpha",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_ALPHA,
            1.0f,
        )
    val strokeLineWidth =
        getNamedFloat(
            a,
            "strokeWidth",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_STROKE_WIDTH,
            1.0f,
        )

    val trimPathEnd =
        getNamedFloat(
            a,
            "trimPathEnd",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_END,
            1.0f,
        )
    val trimPathOffset =
        getNamedFloat(
            a,
            "trimPathOffset",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_OFFSET,
            0.0f,
        )
    val trimPathStart =
        getNamedFloat(
            a,
            "trimPathStart",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_START,
            0.0f,
        )

    val fillRule =
        getNamedInt(
            a,
            "fillType",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_PATH_TRIM_PATH_FILLTYPE,
            FILL_TYPE_WINDING,
        )

    a.recycle()

    val fillPathType = if (fillRule == 0) PathFillType.NonZero else PathFillType.EvenOdd

    builder.addPath(
        pathData,
        fillPathType,
        name,
        fillBrush,
        fillAlpha,
        strokeBrush,
        strokeAlpha,
        strokeLineWidth,
        strokeLineCap,
        strokeLineJoin,
        strokeMiterLimit,
        trimPathStart,
        trimPathEnd,
        trimPathOffset,
    )
}

internal fun AndroidVectorParser.parseClipPath(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet,
    builder: ImageVector.Builder,
) {
    val a =
        obtainAttributes(
            res,
            theme,
            attrs,
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_CLIP_PATH,
        )

    val name: String =
        getString(a, AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_CLIP_PATH_NAME) ?: ""
    val pathStr = getString(a, AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_CLIP_PATH_PATH_DATA)
    val pathData = if (pathStr == null) EmptyPath else pathParser.pathStringToNodes(pathStr)
    a.recycle()

    builder.addGroup(name = name, clipPathData = pathData)
}

internal fun AndroidVectorParser.parseGroup(
    res: Resources,
    theme: Resources.Theme?,
    attrs: AttributeSet,
    builder: ImageVector.Builder,
) {
    val a =
        obtainAttributes(res, theme, attrs, AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP)

    val rotate =
        getNamedFloat(
            a,
            "rotation",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_ROTATION,
            DefaultRotation,
        )

    val pivotX =
        getFloat(a, AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_PIVOT_X, DefaultPivotX)
    val pivotY =
        getFloat(a, AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_PIVOT_Y, DefaultPivotY)

    val scaleX =
        getNamedFloat(
            a,
            "scaleX",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_SCALE_X,
            DefaultScaleX,
        )

    val scaleY =
        getNamedFloat(
            a,
            "scaleY",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_SCALE_Y,
            DefaultScaleY,
        )

    val translateX =
        getNamedFloat(
            a,
            "translateX",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_TRANSLATE_X,
            DefaultTranslationX,
        )
    val translateY =
        getNamedFloat(
            a,
            "translateY",
            AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_TRANSLATE_Y,
            DefaultTranslationY,
        )

    val name: String =
        getString(a, AndroidVectorResources.STYLEABLE_VECTOR_DRAWABLE_GROUP_NAME) ?: ""

    a.recycle()

    builder.addGroup(
        name,
        rotate,
        pivotX,
        pivotY,
        scaleX,
        scaleY,
        translateX,
        translateY,
        EmptyPath,
    )
}

internal class AndroidVectorParser(val xmlParser: XmlPullParser, var config: Int = 0) {
    private var nestedGroupsStack: IntArray? = null
    private var stackPointer = 0

    fun pushGroup(nestedGroups: Int) {
        var stack = nestedGroupsStack
        if (stack == null) {
            stack = IntArray(4)
            nestedGroupsStack = stack
        } else if (stackPointer >= stack.size) {
            stack = stack.copyOf(stack.size * 2)
            nestedGroupsStack = stack
        }
        stack[stackPointer++] = nestedGroups
    }

    fun popGroup(): Int {
        val stack = nestedGroupsStack
        if (stack == null || stackPointer == 0) {
            return 0
        }
        return stack[--stackPointer]
    }

    @JvmField internal val pathParser = PathParser()

    private fun updateConfig(resConfig: Int) {
        config = config or resConfig
    }

    fun obtainAttributes(
        res: Resources,
        theme: Resources.Theme?,
        set: AttributeSet,
        attrs: IntArray,
    ): TypedArray {
        val typedArray = TypedArrayUtils.obtainAttributes(res, theme, set, attrs)
        updateConfig(typedArray.changingConfigurations)
        return typedArray
    }

    fun getNamedInt(
        typedArray: TypedArray,
        attrName: String,
        @StyleableRes resId: Int,
        defaultValue: Int,
    ): Int {
        with(typedArray) {
            val result = TypedArrayUtils.getNamedInt(this, xmlParser, attrName, resId, defaultValue)
            updateConfig(changingConfigurations)
            return result
        }
    }

    fun getNamedFloat(
        typedArray: TypedArray,
        attrName: String,
        @StyleableRes resId: Int,
        defaultValue: Float,
    ): Float {
        with(typedArray) {
            val result =
                TypedArrayUtils.getNamedFloat(this, xmlParser, attrName, resId, defaultValue)
            updateConfig(changingConfigurations)
            return result
        }
    }

    fun getNamedBoolean(
        typedArray: TypedArray,
        attrName: String,
        @StyleableRes resId: Int,
        defaultValue: Boolean,
    ): Boolean {
        with(typedArray) {
            val result =
                TypedArrayUtils.getNamedBoolean(this, xmlParser, attrName, resId, defaultValue)
            updateConfig(changingConfigurations)
            return result
        }
    }

    fun getFloat(typedArray: TypedArray, index: Int, defaultValue: Float): Float {
        with(typedArray) {
            val result = getFloat(index, defaultValue)
            updateConfig(changingConfigurations)
            return result
        }
    }

    fun getInt(typedArray: TypedArray, index: Int, defaultValue: Int): Int {
        with(typedArray) {
            val result = getInt(index, defaultValue)
            updateConfig(changingConfigurations)
            return result
        }
    }

    fun getString(typedArray: TypedArray, index: Int): String? {
        with(typedArray) {
            val result = getString(index)
            updateConfig(changingConfigurations)
            return result
        }
    }

    fun getDimension(typedArray: TypedArray, index: Int, defValue: Float): Float {
        with(typedArray) {
            val result = getDimension(index, defValue)
            updateConfig(changingConfigurations)
            return result
        }
    }

    // TIPS：AndroidX 用 ComplexColorCompat 生成安卓 Shader，Skia 无法使用，这里改为直接产出 Compose Brush
    fun getNamedBrush(
        typedArray: TypedArray,
        res: Resources,
        theme: Resources.Theme?,
        attrName: String,
        @StyleableRes resId: Int,
    ): Brush? {
        with(typedArray) {
            updateConfig(changingConfigurations)
            if (!TypedArrayUtils.hasAttribute(xmlParser, attrName)) return null

            val value = TypedValue()
            if (!getValue(resId, value)) return null

            return when {
                value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT -> value.data.takeIf { it != 0 }?.let { SolidColor(Color(it)) }

                value.resourceId != 0 -> HiroComplexColorParser.parse(res, value.resourceId, theme)

                else -> null
            }
        }
    }

    fun getNamedColorStateList(
        typedArray: TypedArray,
        theme: Resources.Theme?,
        attrName: String,
        @StyleableRes resId: Int,
    ): ColorStateList? {
        with(typedArray) {
            val result =
                TypedArrayUtils.getNamedColorStateList(
                    typedArray,
                    xmlParser,
                    theme,
                    attrName,
                    resId,
                )
            updateConfig(changingConfigurations)
            return result
        }
    }
}
