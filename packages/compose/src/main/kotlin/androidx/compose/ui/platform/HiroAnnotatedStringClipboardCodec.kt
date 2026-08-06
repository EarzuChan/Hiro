package androidx.compose.ui.platform

import android.os.Parcel
import android.text.Annotation
import android.text.SpannableString
import android.text.Spanned
import android.util.Base64
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType

internal object HiroAnnotatedStringClipboardCodec {
    fun encode(value: AnnotatedString): CharSequence {
        if (value.spanStyles.isEmpty()) return value.text
        return SpannableString(value.text).apply {
            value.spanStyles.forEach { range ->
                setSpan(
                    Annotation(SPAN_STYLE_KEY, encodeStyle(range.item)),
                    range.start,
                    range.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
    }

    fun decode(value: CharSequence?): AnnotatedString? {
        value ?: return null
        if (value !is Spanned) return AnnotatedString(value.toString())
        val styles = value.getSpans(0, value.length, Annotation::class.java).mapNotNull { annotation ->
            if (annotation.key != SPAN_STYLE_KEY) return@mapNotNull null
            val style = try {
                decodeStyle(annotation.value)
            } catch (exception: RuntimeException) {
                Log.w(TAG, "剪贴板中的 Compose 文本样式已损坏，本段样式已忽略", exception)
                return@mapNotNull null
            }
            AnnotatedString.Range(style, value.getSpanStart(annotation), value.getSpanEnd(annotation))
        }
        return AnnotatedString(value.toString(), spanStyles = styles)
    }

    private fun encodeStyle(style: SpanStyle): String = withParcel { parcel ->
        if (style.color != Color.Unspecified) parcel.writeField(COLOR_ID) { writeLong(style.color.value.toLong()) }
        if (style.fontSize != TextUnit.Unspecified) parcel.writeField(FONT_SIZE_ID) { writeTextUnit(style.fontSize) }
        style.fontWeight?.let { value -> parcel.writeField(FONT_WEIGHT_ID) { writeInt(value.weight) } }
        style.fontStyle?.let { value -> parcel.writeField(FONT_STYLE_ID) { writeByte(if (value == FontStyle.Italic) 1 else 0) } }
        style.fontSynthesis?.let { value -> parcel.writeField(FONT_SYNTHESIS_ID) { writeByte(value.toEncodedByte()) } }
        style.fontFeatureSettings?.let { value -> parcel.writeField(FONT_FEATURE_SETTINGS_ID) { writeString(value) } }
        if (style.letterSpacing != TextUnit.Unspecified) parcel.writeField(LETTER_SPACING_ID) { writeTextUnit(style.letterSpacing) }
        style.baselineShift?.let { value -> parcel.writeField(BASELINE_SHIFT_ID) { writeFloat(value.multiplier) } }
        style.textGeometricTransform?.let { value ->
            parcel.writeField(TEXT_GEOMETRIC_TRANSFORM_ID) {
                writeFloat(value.scaleX)
                writeFloat(value.skewX)
            }
        }
        if (style.background != Color.Unspecified) parcel.writeField(BACKGROUND_ID) { writeLong(style.background.value.toLong()) }
        style.textDecoration?.let { value -> parcel.writeField(TEXT_DECORATION_ID) { writeInt(value.toEncodedMask()) } }
        style.shadow?.let { value ->
            parcel.writeField(SHADOW_ID) {
                writeLong(value.color.value.toLong())
                writeFloat(value.offset.x)
                writeFloat(value.offset.y)
                writeFloat(value.blurRadius)
            }
        }
        Base64.encodeToString(parcel.marshall(), Base64.DEFAULT)
    }

    private fun decodeStyle(encoded: String): SpanStyle = withDecodedParcel(encoded) { parcel ->
        var style = SpanStyle()
        while (parcel.dataAvail() > BYTE_SIZE) {
            style = when (parcel.readByte()) {
                COLOR_ID -> if (parcel.has(COLOR_SIZE)) style.copy(color = Color(parcel.readLong().toULong())) else break
                FONT_SIZE_ID -> if (parcel.has(TEXT_UNIT_SIZE)) style.copy(fontSize = parcel.readTextUnit()) else break
                FONT_WEIGHT_ID -> if (parcel.has(INT_SIZE)) style.copy(fontWeight = FontWeight(parcel.readInt())) else break
                FONT_STYLE_ID -> if (parcel.has(BYTE_SIZE)) style.copy(fontStyle = if (parcel.readByte() == 1.toByte()) FontStyle.Italic else FontStyle.Normal) else break
                FONT_SYNTHESIS_ID -> if (parcel.has(BYTE_SIZE)) style.copy(fontSynthesis = parcel.readFontSynthesis()) else break
                FONT_FEATURE_SETTINGS_ID -> style.copy(fontFeatureSettings = parcel.readString())
                LETTER_SPACING_ID -> if (parcel.has(TEXT_UNIT_SIZE)) style.copy(letterSpacing = parcel.readTextUnit()) else break
                BASELINE_SHIFT_ID -> if (parcel.has(FLOAT_SIZE)) style.copy(baselineShift = BaselineShift(parcel.readFloat())) else break
                TEXT_GEOMETRIC_TRANSFORM_ID -> if (parcel.has(FLOAT_SIZE * 2)) {
                    style.copy(textGeometricTransform = TextGeometricTransform(parcel.readFloat(), parcel.readFloat()))
                } else break
                BACKGROUND_ID -> if (parcel.has(COLOR_SIZE)) style.copy(background = Color(parcel.readLong().toULong())) else break
                TEXT_DECORATION_ID -> if (parcel.has(INT_SIZE)) style.copy(textDecoration = parcel.readTextDecoration()) else break
                SHADOW_ID -> if (parcel.has(SHADOW_SIZE)) {
                    style.copy(
                        shadow = Shadow(
                            color = Color(parcel.readLong().toULong()),
                            offset = Offset(parcel.readFloat(), parcel.readFloat()),
                            blurRadius = parcel.readFloat(),
                        )
                    )
                } else break
                else -> break
            }
        }
        style
    }

    private inline fun <T> withParcel(block: (Parcel) -> T): T {
        val parcel = Parcel.obtain()
        return try {
            block(parcel)
        } finally {
            parcel.recycle()
        }
    }

    private inline fun <T> withDecodedParcel(encoded: String, block: (Parcel) -> T): T = withParcel { parcel ->
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        parcel.unmarshall(bytes, 0, bytes.size)
        parcel.setDataPosition(0)
        block(parcel)
    }

    private inline fun Parcel.writeField(id: Byte, value: Parcel.() -> Unit) {
        writeByte(id)
        value()
    }

    private fun Parcel.writeTextUnit(value: TextUnit) {
        writeByte(
            when (value.type) {
                TextUnitType.Sp -> UNIT_TYPE_SP
                TextUnitType.Em -> UNIT_TYPE_EM
                else -> UNIT_TYPE_UNSPECIFIED
            }
        )
        if (value.type != TextUnitType.Unspecified) writeFloat(value.value)
    }

    private fun Parcel.readTextUnit(): TextUnit {
        val type = when (readByte()) {
            UNIT_TYPE_SP -> TextUnitType.Sp
            UNIT_TYPE_EM -> TextUnitType.Em
            else -> TextUnitType.Unspecified
        }
        return if (type == TextUnitType.Unspecified) TextUnit.Unspecified else TextUnit(readFloat(), type)
    }

    private fun FontSynthesis.toEncodedByte(): Byte = when (this) {
        FontSynthesis.All -> FONT_SYNTHESIS_ALL
        FontSynthesis.Weight -> FONT_SYNTHESIS_WEIGHT
        FontSynthesis.Style -> FONT_SYNTHESIS_STYLE
        else -> FONT_SYNTHESIS_NONE
    }

    private fun Parcel.readFontSynthesis(): FontSynthesis = when (readByte()) {
        FONT_SYNTHESIS_ALL -> FontSynthesis.All
        FONT_SYNTHESIS_WEIGHT -> FontSynthesis.Weight
        FONT_SYNTHESIS_STYLE -> FontSynthesis.Style
        else -> FontSynthesis.None
    }

    private fun TextDecoration.toEncodedMask(): Int =
        (if (TextDecoration.Underline in this) UNDERLINE_MASK else 0) or
            (if (TextDecoration.LineThrough in this) LINE_THROUGH_MASK else 0)

    private fun Parcel.readTextDecoration(): TextDecoration = when (readInt()) {
        UNDERLINE_MASK -> TextDecoration.Underline
        LINE_THROUGH_MASK -> TextDecoration.LineThrough
        UNDERLINE_MASK or LINE_THROUGH_MASK -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
        else -> TextDecoration.None
    }

    private fun Parcel.has(size: Int): Boolean = dataAvail() >= size

    private const val TAG = "HiroClipboardCodec"
    private const val SPAN_STYLE_KEY = "androidx.compose.text.SpanStyle"
    private const val UNIT_TYPE_UNSPECIFIED: Byte = 0
    private const val UNIT_TYPE_SP: Byte = 1
    private const val UNIT_TYPE_EM: Byte = 2
    private const val FONT_SYNTHESIS_NONE: Byte = 0
    private const val FONT_SYNTHESIS_ALL: Byte = 1
    private const val FONT_SYNTHESIS_WEIGHT: Byte = 2
    private const val FONT_SYNTHESIS_STYLE: Byte = 3
    private const val COLOR_ID: Byte = 1
    private const val FONT_SIZE_ID: Byte = 2
    private const val FONT_WEIGHT_ID: Byte = 3
    private const val FONT_STYLE_ID: Byte = 4
    private const val FONT_SYNTHESIS_ID: Byte = 5
    private const val FONT_FEATURE_SETTINGS_ID: Byte = 6
    private const val LETTER_SPACING_ID: Byte = 7
    private const val BASELINE_SHIFT_ID: Byte = 8
    private const val TEXT_GEOMETRIC_TRANSFORM_ID: Byte = 9
    private const val BACKGROUND_ID: Byte = 10
    private const val TEXT_DECORATION_ID: Byte = 11
    private const val SHADOW_ID: Byte = 12
    private const val UNDERLINE_MASK = 1
    private const val LINE_THROUGH_MASK = 2
    private const val BYTE_SIZE = 1
    private const val INT_SIZE = 4
    private const val FLOAT_SIZE = 4
    private const val COLOR_SIZE = 8
    private const val TEXT_UNIT_SIZE = BYTE_SIZE + FLOAT_SIZE
    private const val SHADOW_SIZE = COLOR_SIZE + FLOAT_SIZE * 3
}
