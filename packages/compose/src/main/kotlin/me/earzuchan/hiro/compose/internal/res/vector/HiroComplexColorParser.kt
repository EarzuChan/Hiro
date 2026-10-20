// TIPS：与 AndroidX 原版一致，使用 androidx.core 中限定库组使用的 TypedArrayUtils
@file:Suppress("RestrictedApi")

package me.earzuchan.hiro.compose.internal.res.vector

import android.content.res.Resources
import android.util.Xml
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.core.R as CoreR
import androidx.core.content.res.TypedArrayUtils
import org.xmlpull.v1.XmlPullParser

// TIPS：把安卓的复合颜色资源（gradient 或 selector）转换为 Compose Brush，规则与 AndroidX GradientColorInflaterCompat 对齐
internal object HiroComplexColorParser {
    private const val TYPE_LINEAR = 0
    private const val TYPE_RADIAL = 1
    private const val TYPE_SWEEP = 2

    private const val TILE_MODE_REPEAT = 1
    private const val TILE_MODE_MIRROR = 2

    fun parse(res: Resources, resId: Int, theme: Resources.Theme?): Brush? {
        val parser = res.getXml(resId)
        parser.use {
            var type = it.next()
            while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) type = it.next()
            if (type != XmlPullParser.START_TAG) return null

            return when (it.name) {
                "gradient" -> parseGradient(res, it, theme)

                // TIPS：selector 在矢量图中没有状态可言，取其默认颜色
                else -> res.getColorStateList(resId, theme).defaultColor.takeIf { color -> color != 0 }?.let { color -> SolidColor(Color(color)) }
            }
        }
    }

    private fun parseGradient(res: Resources, parser: XmlPullParser, theme: Resources.Theme?): Brush {
        val attrs = Xml.asAttributeSet(parser)
        val a = TypedArrayUtils.obtainAttributes(res, theme, attrs, CoreR.styleable.GradientColor)

        val startX = TypedArrayUtils.getNamedFloat(a, parser, "startX", CoreR.styleable.GradientColor_android_startX, 0f)
        val startY = TypedArrayUtils.getNamedFloat(a, parser, "startY", CoreR.styleable.GradientColor_android_startY, 0f)
        val endX = TypedArrayUtils.getNamedFloat(a, parser, "endX", CoreR.styleable.GradientColor_android_endX, 0f)
        val endY = TypedArrayUtils.getNamedFloat(a, parser, "endY", CoreR.styleable.GradientColor_android_endY, 0f)
        val centerX = TypedArrayUtils.getNamedFloat(a, parser, "centerX", CoreR.styleable.GradientColor_android_centerX, 0f)
        val centerY = TypedArrayUtils.getNamedFloat(a, parser, "centerY", CoreR.styleable.GradientColor_android_centerY, 0f)
        val gradientType = TypedArrayUtils.getNamedInt(a, parser, "type", CoreR.styleable.GradientColor_android_type, TYPE_LINEAR)
        val startColor = TypedArrayUtils.getNamedColor(a, parser, "startColor", CoreR.styleable.GradientColor_android_startColor, 0)
        val hasCenterColor = TypedArrayUtils.hasAttribute(parser, "centerColor")
        val centerColor = TypedArrayUtils.getNamedColor(a, parser, "centerColor", CoreR.styleable.GradientColor_android_centerColor, 0)
        val endColor = TypedArrayUtils.getNamedColor(a, parser, "endColor", CoreR.styleable.GradientColor_android_endColor, 0)
        val tileModeValue = TypedArrayUtils.getNamedInt(a, parser, "tileMode", CoreR.styleable.GradientColor_android_tileMode, 0)
        val gradientRadius = TypedArrayUtils.getNamedFloat(a, parser, "gradientRadius", CoreR.styleable.GradientColor_android_gradientRadius, 0f)
        a.recycle()

        val stops = parseItems(res, parser, theme).ifEmpty {
            if (hasCenterColor) listOf(0f to Color(startColor), 0.5f to Color(centerColor), 1f to Color(endColor))
            else listOf(0f to Color(startColor), 1f to Color(endColor))
        }.toTypedArray()

        val tileMode = when (tileModeValue) {
            TILE_MODE_REPEAT -> TileMode.Repeated

            TILE_MODE_MIRROR -> TileMode.Mirror

            else -> TileMode.Clamp
        }

        return when (gradientType) {
            TYPE_RADIAL -> {
                require(gradientRadius > 0f) { "径向渐变的 gradientRadius 必须大于 0" }
                Brush.radialGradient(*stops, center = Offset(centerX, centerY), radius = gradientRadius, tileMode = tileMode)
            }

            TYPE_SWEEP -> Brush.sweepGradient(*stops, center = Offset(centerX, centerY))

            else -> Brush.linearGradient(*stops, start = Offset(startX, startY), end = Offset(endX, endY), tileMode = tileMode)
        }
    }

    private fun parseItems(res: Resources, parser: XmlPullParser, theme: Resources.Theme?): List<Pair<Float, Color>> {
        val items = mutableListOf<Pair<Float, Color>>()
        val innerDepth = parser.depth + 1

        var type = parser.next()
        while (type != XmlPullParser.END_DOCUMENT && (parser.depth >= innerDepth || type != XmlPullParser.END_TAG)) {
            if (type == XmlPullParser.START_TAG && parser.depth <= innerDepth && parser.name == "item") {
                val a = TypedArrayUtils.obtainAttributes(res, theme, Xml.asAttributeSet(parser), CoreR.styleable.GradientColorItem)
                require(a.hasValue(CoreR.styleable.GradientColorItem_android_color) && a.hasValue(CoreR.styleable.GradientColorItem_android_offset)) { "${parser.positionDescription}：渐变的 <item> 必须同时声明 color 与 offset" }

                items += a.getFloat(CoreR.styleable.GradientColorItem_android_offset, 0f) to Color(a.getColor(CoreR.styleable.GradientColorItem_android_color, 0))
                a.recycle()
            }
            type = parser.next()
        }

        return items
    }
}
