package me.earzuchan.hiro.compose.internal.res

import android.content.res.Resources
import android.content.res.TypedArray
import android.content.res.XmlResourceParser
import android.util.AttributeSet
import android.util.TypedValue
import android.util.Xml
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min
import org.xmlpull.v1.XmlPullParser

/** 安卓 <shape>（GradientDrawable）的描述，尺寸均以 Dp 保存，绘制时按 Hiro 场景密度换算 */
internal class HiroShapeSpec(
    val shape: Int,
    val cornerRadii: List<Dp>,
    val solidColor: Color?,
    val gradient: Gradient?,
    val strokeWidth: Dp,
    val strokeColor: Color?,
    val strokeDashWidth: Dp,
    val strokeDashGap: Dp,
    val width: Dp?,
    val height: Dp?,
) {
    class Gradient(val type: Int, val angle: Int, val colors: List<Color>, val positions: List<Float>?, val centerX: Float, val centerY: Float, val radius: Dp?, val radiusFraction: Float?)

    companion object {
        const val RECTANGLE = 0
        const val OVAL = 1
        const val LINE = 2
        const val RING = 3

        const val GRADIENT_LINEAR = 0
        const val GRADIENT_RADIAL = 1
        const val GRADIENT_SWEEP = 2
    }
}

internal object HiroShapeParser {
    fun parse(res: Resources, theme: Resources.Theme?, parser: XmlResourceParser): HiroShapeSpec {
        val density = res.displayMetrics.density
        fun Float.toDp() = (this / density).dp

        val root = HiroAttrs.obtain(res, theme, Xml.asAttributeSet(parser), android.R.attr.shape)
        val shape = root.int(android.R.attr.shape, HiroShapeSpec.RECTANGLE)
        root.recycle()

        var radii = List(4) { 0.dp }
        var solidColor: Color? = null
        var gradient: HiroShapeSpec.Gradient? = null
        var strokeWidth = 0.dp
        var strokeColor: Color? = null
        var dashWidth = 0.dp
        var dashGap = 0.dp
        var width: Dp? = null
        var height: Dp? = null

        val innerDepth = parser.depth + 1
        var type = parser.next()
        while (type != XmlPullParser.END_DOCUMENT && (parser.depth >= innerDepth || type != XmlPullParser.END_TAG)) {
            if (type == XmlPullParser.START_TAG && parser.depth <= innerDepth) {
                val attrs = Xml.asAttributeSet(parser)
                when (parser.name) {
                    "solid" -> HiroAttrs.obtain(res, theme, attrs, android.R.attr.color).use { solidColor = it.color(android.R.attr.color) }

                    "stroke" -> HiroAttrs.obtain(res, theme, attrs, android.R.attr.width, android.R.attr.color, android.R.attr.dashWidth, android.R.attr.dashGap).use {
                        strokeWidth = it.dimension(android.R.attr.width, 0f).toDp()
                        strokeColor = it.color(android.R.attr.color)
                        dashWidth = it.dimension(android.R.attr.dashWidth, 0f).toDp()
                        dashGap = it.dimension(android.R.attr.dashGap, 0f).toDp()
                    }

                    "corners" -> HiroAttrs.obtain(res, theme, attrs, android.R.attr.radius, android.R.attr.topLeftRadius, android.R.attr.topRightRadius, android.R.attr.bottomRightRadius, android.R.attr.bottomLeftRadius).use {
                        val radius = it.dimension(android.R.attr.radius, 0f)
                        radii = listOf(android.R.attr.topLeftRadius, android.R.attr.topRightRadius, android.R.attr.bottomRightRadius, android.R.attr.bottomLeftRadius).map { attr -> it.dimension(attr, radius).toDp() }
                    }

                    "size" -> HiroAttrs.obtain(res, theme, attrs, android.R.attr.width, android.R.attr.height).use {
                        width = it.dimension(android.R.attr.width, -1f).takeIf { value -> value >= 0f }?.toDp()
                        height = it.dimension(android.R.attr.height, -1f).takeIf { value -> value >= 0f }?.toDp()
                    }

                    "gradient" -> gradient = parseGradient(res, theme, attrs, density)
                }
            }
            type = parser.next()
        }

        return HiroShapeSpec(shape, radii, solidColor, gradient, strokeWidth, strokeColor, dashWidth, dashGap, width, height)
    }

    private fun parseGradient(res: Resources, theme: Resources.Theme?, attrs: AttributeSet, density: Float): HiroShapeSpec.Gradient = HiroAttrs.obtain(
        res, theme, attrs,
        android.R.attr.type, android.R.attr.angle, android.R.attr.startColor, android.R.attr.centerColor, android.R.attr.endColor,
        android.R.attr.centerX, android.R.attr.centerY, android.R.attr.gradientRadius,
    ).use {
        val start = it.color(android.R.attr.startColor) ?: Color.Transparent
        val center = it.color(android.R.attr.centerColor)
        val end = it.color(android.R.attr.endColor) ?: Color.Transparent
        val centerX = it.fraction(android.R.attr.centerX, 0.5f)
        val centerY = it.fraction(android.R.attr.centerY, 0.5f)

        val radiusValue = it.peek(android.R.attr.gradientRadius)
        val radiusFraction = if (radiusValue?.type == TypedValue.TYPE_FRACTION) it.fraction(android.R.attr.gradientRadius, 0.5f) else null
        val radius = if (radiusValue != null && radiusFraction == null) (it.dimension(android.R.attr.gradientRadius, 0f) / density).dp else null

        HiroShapeSpec.Gradient(
            type = it.int(android.R.attr.type, HiroShapeSpec.GRADIENT_LINEAR),
            angle = ((it.float(android.R.attr.angle, 0f).toInt() % 360) + 360) % 360,
            colors = listOfNotNull(start, center, end),
            positions = if (center != null) listOf(0f, if (centerX != 0.5f) centerX else centerY, 1f) else null,
            centerX = centerX,
            centerY = centerY,
            radius = radius,
            radiusFraction = radiusFraction,
        )
    }
}

/** 按 GradientDrawable 的规则绘制 <shape>，固有尺寸按创建时的 Hiro 场景密度换算 */
internal class HiroShapePainter(private val spec: HiroShapeSpec, private val density: Density) : Painter() {
    private var alpha = 1f
    private var colorFilter: ColorFilter? = null

    override val intrinsicSize: Size = if (spec.width == null && spec.height == null) Size.Unspecified else with(density) { Size(spec.width?.toPx() ?: Float.NaN, spec.height?.toPx() ?: Float.NaN) }

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {
        val bounds = Rect(Offset.Zero, size)
        val strokeWidthPx = spec.strokeWidth.toPx()

        val fill: Brush? = spec.gradient?.toBrush(bounds, this) ?: spec.solidColor?.let(::SolidColor)
        fill?.let { drawShape(bounds, it, Fill) }

        if (strokeWidthPx > 0f && spec.strokeColor != null) {
            val pathEffect = if (spec.strokeDashWidth.value > 0f) PathEffect.dashPathEffect(floatArrayOf(spec.strokeDashWidth.toPx(), spec.strokeDashGap.toPx())) else null
            val half = strokeWidthPx / 2f
            drawShape(bounds.deflate(half), SolidColor(spec.strokeColor), Stroke(width = strokeWidthPx, pathEffect = pathEffect))
        }
    }

    private fun DrawScope.drawShape(rect: Rect, brush: Brush, style: androidx.compose.ui.graphics.drawscope.DrawStyle) {
        when (spec.shape) {
            HiroShapeSpec.OVAL, HiroShapeSpec.RING -> drawOval(brush, rect.topLeft, rect.size, alpha, style, colorFilter)

            HiroShapeSpec.LINE -> if (style is Stroke) drawLine(brush, Offset(rect.left, rect.center.y), Offset(rect.right, rect.center.y), style.width, pathEffect = style.pathEffect, alpha = alpha, colorFilter = colorFilter)

            else -> {
                val radii = spec.cornerRadii.map { it.toPx() }
                if (radii.all { it <= 0f }) drawRect(brush, rect.topLeft, rect.size, alpha, style, colorFilter)
                else drawPath(Path().apply { addRoundRect(RoundRect(rect, CornerRadius(radii[0]), CornerRadius(radii[1]), CornerRadius(radii[2]), CornerRadius(radii[3]))) }, brush, alpha, style, colorFilter)
            }
        }
    }

    private fun HiroShapeSpec.Gradient.toBrush(rect: Rect, scope: DrawScope): Brush {
        val stops = if (positions != null) positions.zip(colors).toTypedArray() else colors.mapIndexed { index, color -> index.toFloat() / (colors.size - 1) to color }.toTypedArray()
        val center = Offset(rect.left + rect.width * centerX, rect.top + rect.height * centerY)

        return when (type) {
            HiroShapeSpec.GRADIENT_RADIAL -> {
                val radiusPx = radiusFraction?.let { it * min(rect.width, rect.height) } ?: with(scope) { radius?.toPx() } ?: (min(rect.width, rect.height) / 2f)
                Brush.radialGradient(*stops, center = center, radius = radiusPx.coerceAtLeast(0.01f))
            }

            HiroShapeSpec.GRADIENT_SWEEP -> Brush.sweepGradient(*stops, center = center)

            else -> {
                // 与 GradientDrawable 一致：0 为左到右，逆时针每 45 度一档
                val (start, end) = when (angle / 45 * 45) {
                    45 -> rect.bottomLeft to rect.topRight
                    90 -> Offset(rect.left, rect.bottom) to Offset(rect.left, rect.top)
                    135 -> rect.bottomRight to rect.topLeft
                    180 -> Offset(rect.right, rect.top) to Offset(rect.left, rect.top)
                    225 -> rect.topRight to rect.bottomLeft
                    270 -> Offset(rect.left, rect.top) to Offset(rect.left, rect.bottom)
                    315 -> rect.topLeft to rect.bottomRight
                    else -> Offset(rect.left, rect.top) to Offset(rect.right, rect.top)
                }
                Brush.linearGradient(*stops, start = start, end = end)
            }
        }
    }
}

/** 以任意顺序的安卓属性 ID 读取 TypedArray，内部排序以满足 obtainAttributes 的要求，并解析主题属性 */
internal class HiroAttrs private constructor(private val array: TypedArray, private val indexOf: Map<Int, Int>) {
    companion object {
        fun obtain(res: Resources, theme: Resources.Theme?, set: AttributeSet, vararg attrs: Int): HiroAttrs {
            val sorted = attrs.sortedArray()
            val array = theme?.obtainStyledAttributes(set, sorted, 0, 0) ?: res.obtainAttributes(set, sorted)
            return HiroAttrs(array, sorted.withIndex().associate { (index, attr) -> attr to index })
        }
    }

    private fun index(attr: Int) = indexOf.getValue(attr)

    fun peek(attr: Int): TypedValue? = array.peekValue(index(attr))

    fun int(attr: Int, default: Int) = array.getInt(index(attr), default)

    fun float(attr: Int, default: Float) = array.getFloat(index(attr), default)

    fun dimension(attr: Int, default: Float) = array.getDimension(index(attr), default)

    fun fraction(attr: Int, default: Float): Float {
        val value = peek(attr) ?: return default
        return if (value.type == TypedValue.TYPE_FRACTION) value.getFraction(1f, 1f) else array.getFloat(index(attr), default)
    }

    fun color(attr: Int): Color? = if (!array.hasValue(index(attr))) null else array.getColorStateList(index(attr))?.defaultColor?.let(::Color)

    fun recycle() = array.recycle()

    inline fun <T> use(block: (HiroAttrs) -> T): T = try {
        block(this)
    } finally {
        recycle()
    }
}
