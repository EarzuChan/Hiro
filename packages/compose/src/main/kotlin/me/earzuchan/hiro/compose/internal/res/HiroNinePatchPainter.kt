package me.earzuchan.hiro.compose.internal.res

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/** 编译后 .9.png 中的拉伸信息，xDivs、yDivs 成对表示可拉伸区间的起止像素 */
internal class HiroNinePatchChunk(val xDivs: IntArray, val yDivs: IntArray) {
    companion object {
        // TIPS：格式即 AOSP Res_png_9patch 序列化形态，32 字节头之后依次是 xDivs、yDivs、colors，字节序与设备一致
        fun parse(chunk: ByteArray): HiroNinePatchChunk {
            val buffer = ByteBuffer.wrap(chunk).order(ByteOrder.nativeOrder())
            buffer.get() // 是否已反序列化
            val numXDivs = buffer.get().toInt() and 0xFF
            val numYDivs = buffer.get().toInt() and 0xFF
            buffer.get() // 颜色数
            buffer.position(32)

            return HiroNinePatchChunk(IntArray(numXDivs) { buffer.getInt() }, IntArray(numYDivs) { buffer.getInt() })
        }
    }
}

/** 按 Android 九宫格规则绘制的 Painter：固定段保持原尺寸，剩余空间按比例分给各拉伸段 */
internal class HiroNinePatchPainter(private val image: ImageBitmap, private val chunk: HiroNinePatchChunk) : Painter() {
    private var alpha = 1f
    private var colorFilter: ColorFilter? = null

    override val intrinsicSize: Size get() = Size(image.width.toFloat(), image.height.toFloat())

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {
        val columns = segments(image.width, chunk.xDivs, size.width)
        val rows = segments(image.height, chunk.yDivs, size.height)

        for (row in rows) for (column in columns) {
            if (column.dstEnd <= column.dstStart || row.dstEnd <= row.dstStart || column.srcEnd <= column.srcStart || row.srcEnd <= row.srcStart) continue

            drawImage(
                image = image,
                srcOffset = IntOffset(column.srcStart, row.srcStart),
                srcSize = IntSize(column.srcEnd - column.srcStart, row.srcEnd - row.srcStart),
                dstOffset = IntOffset(column.dstStart, row.dstStart),
                dstSize = IntSize(column.dstEnd - column.dstStart, row.dstEnd - row.dstStart),
                alpha = alpha,
                colorFilter = colorFilter,
            )
        }
    }

    private class Segment(val srcStart: Int, val srcEnd: Int, val dstStart: Int, val dstEnd: Int)

    private fun segments(sourceLength: Int, divs: IntArray, targetLength: Float): List<Segment> {
        // 交替排列的源区间：false 为固定段，true 为拉伸段
        val bounds = mutableListOf<Triple<Int, Int, Boolean>>()
        var cursor = 0
        var index = 0
        while (index + 1 < divs.size) {
            val start = divs[index].coerceIn(cursor, sourceLength)
            val end = divs[index + 1].coerceIn(start, sourceLength)
            if (start > cursor) bounds += Triple(cursor, start, false)
            if (end > start) bounds += Triple(start, end, true)
            cursor = end
            index += 2
        }
        if (cursor < sourceLength) bounds += Triple(cursor, sourceLength, bounds.isEmpty())

        val stretchTotal = bounds.filter { it.third }.sumOf { it.second - it.first }
        val fixedTotal = sourceLength - stretchTotal
        val fixedScale = if (fixedTotal > 0 && targetLength < fixedTotal) targetLength / fixedTotal else 1f
        val remaining = (targetLength - fixedTotal * fixedScale).coerceAtLeast(0f)

        var position = 0f
        return bounds.map { (start, end, stretch) ->
            val length = end - start
            val dstLength = if (stretch) (if (stretchTotal > 0) remaining * length / stretchTotal else 0f) else length * fixedScale
            val dstStart = position.roundToInt()
            position += dstLength
            Segment(start, end, dstStart, position.roundToInt())
        }
    }
}
