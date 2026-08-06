package me.earzuchan.hiro.compose.internal.input

import android.graphics.Matrix
import android.graphics.RectF
import android.os.Build
import android.view.View
import android.view.inputmethod.CursorAnchorInfo
import android.view.inputmethod.EditorBoundsInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix as ComposeMatrix
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection

internal data class HiroCursorAnchorInfoRequest(
    val monitor: Boolean,
    val includeInsertionMarker: Boolean,
    val includeCharacterBounds: Boolean,
    val includeEditorBounds: Boolean,
    val includeLineBounds: Boolean,
)

internal fun resolveCursorAnchorInfoRequest(mode: Int, separateFilter: Int?): HiroCursorAnchorInfoRequest? {
    val knownModes = InputConnection.CURSOR_UPDATE_IMMEDIATE or InputConnection.CURSOR_UPDATE_MONITOR
    var knownFilters = 0
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        knownFilters = knownFilters or InputConnection.CURSOR_UPDATE_FILTER_INSERTION_MARKER or
            InputConnection.CURSOR_UPDATE_FILTER_CHARACTER_BOUNDS or
            InputConnection.CURSOR_UPDATE_FILTER_EDITOR_BOUNDS
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        knownFilters = knownFilters or InputConnection.CURSOR_UPDATE_FILTER_VISIBLE_LINE_BOUNDS
    }
    val knownFlags = knownModes or knownFilters
    val combined = if (separateFilter == null) mode else {
        if (mode and knownModes.inv() != 0 || separateFilter and knownFilters.inv() != 0) return null
        mode or separateFilter
    }
    if (combined and knownFlags.inv() != 0) return null

    var insertionMarker = true
    var characterBounds = true
    var editorBounds = false
    var lineBounds = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        insertionMarker = combined and InputConnection.CURSOR_UPDATE_FILTER_INSERTION_MARKER != 0
        characterBounds = combined and InputConnection.CURSOR_UPDATE_FILTER_CHARACTER_BOUNDS != 0
        editorBounds = combined and InputConnection.CURSOR_UPDATE_FILTER_EDITOR_BOUNDS != 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            lineBounds = combined and InputConnection.CURSOR_UPDATE_FILTER_VISIBLE_LINE_BOUNDS != 0
        }
        if (separateFilter == null && !insertionMarker && !characterBounds && !editorBounds && !lineBounds) {
            insertionMarker = true
            characterBounds = true
            editorBounds = true
            lineBounds = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        }
    }
    return HiroCursorAnchorInfoRequest(
        monitor = combined and InputConnection.CURSOR_UPDATE_MONITOR != 0,
        includeInsertionMarker = insertionMarker,
        includeCharacterBounds = characterBounds,
        includeEditorBounds = editorBounds,
        includeLineBounds = lineBounds,
    )
}

internal fun buildHiroCursorAnchorInfo(
    view: View,
    snapshot: HiroImeSnapshot,
    request: HiroCursorAnchorInfoRequest,
): CursorAnchorInfo? {
    val matrix = Matrix().also { view.calculateLocalToScreenMatrix(it) }
    val layout = snapshot.textLayoutResult
    val textTransform = snapshot.textLayoutToRootTransform
    val clippingBounds = snapshot.textClippingRectInText
    val needsTextGeometry = request.includeInsertionMarker || request.includeCharacterBounds || request.includeLineBounds
    if ((needsTextGeometry || request.includeEditorBounds) && textTransform == null) return null
    if (needsTextGeometry && (layout == null || clippingBounds == null)) return null
    if (request.includeEditorBounds && snapshot.textFieldRectInText == null) return null
    textTransform?.let { matrix.preConcat(it.toAndroidMatrix()) }
    val builder = CursorAnchorInfo.Builder()
        .setMatrix(matrix)
        .setSelectionRange(snapshot.value.selection.min, snapshot.value.selection.max)

    if (layout != null && clippingBounds != null) {
        if (request.includeInsertionMarker) builder.setInsertionMarker(snapshot, layout, clippingBounds)
        if (request.includeCharacterBounds) builder.setCompositionBounds(snapshot, layout, clippingBounds)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && request.includeLineBounds) {
            HiroCursorAnchorInfoApi34.addVisibleLineBounds(builder, layout, clippingBounds)
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && request.includeEditorBounds) {
        snapshot.textFieldRectInText?.let { HiroCursorAnchorInfoApi33.setEditorBounds(builder, it) }
    }
    return builder.build()
}

private fun CursorAnchorInfo.Builder.setInsertionMarker(
    snapshot: HiroImeSnapshot,
    layout: TextLayoutResult,
    clippingBounds: Rect,
) {
    val originalOffset = snapshot.value.selection.min
    val transformedOffset = snapshot.offsetMapping.originalToTransformed(originalOffset)
        .coerceIn(0, layout.layoutInput.text.length)
    val localCursor = layout.getCursorRect(transformedOffset)
    val cursorX = localCursor.left.coerceIn(0f, layout.size.width.toFloat())
    val cursor = Rect(cursorX, localCursor.top, cursorX, localCursor.bottom)
    val line = layout.getLineForOffset(transformedOffset.coerceAtMost((layout.layoutInput.text.length - 1).coerceAtLeast(0)))
    val baseline = layout.getLineBaseline(line)
    var flags = insertionVisibilityFlags(cursor.left, cursor.top, cursor.bottom, clippingBounds)
    if (layout.isRtlAt(transformedOffset)) flags = flags or CursorAnchorInfo.FLAG_IS_RTL
    setInsertionMarkerLocation(cursor.left, cursor.top, baseline, cursor.bottom, flags)
}

private fun CursorAnchorInfo.Builder.setCompositionBounds(
    snapshot: HiroImeSnapshot,
    layout: TextLayoutResult,
    clippingBounds: Rect,
) {
    val composition = snapshot.value.composition ?: return
    val start = composition.min.coerceIn(0, snapshot.value.text.length)
    val end = composition.max.coerceIn(start, snapshot.value.text.length)
    if (start == end) return
    setComposingText(start, snapshot.value.text.subSequence(start, end))

    val layoutLength = layout.layoutInput.text.length
    if (layoutLength == 0) return
    for (offset in start until end) {
        val transformed = snapshot.offsetMapping.originalToTransformed(offset).coerceIn(0, layoutLength - 1)
        val bounds = layout.getBoundingBox(transformed)
        var flags = visibilityFlags(bounds, clippingBounds)
        if (layout.isRtlAt(transformed)) flags = flags or CursorAnchorInfo.FLAG_IS_RTL
        addCharacterBounds(offset, bounds.left, bounds.top, bounds.right, bounds.bottom, flags)
    }
}

private fun TextLayoutResult.isRtlAt(offset: Int): Boolean {
    val textLength = layoutInput.text.length
    if (textLength == 0) return false
    return getBidiRunDirection(offset.coerceIn(0, textLength - 1)) == ResolvedTextDirection.Rtl
}

private fun insertionVisibilityFlags(x: Float, top: Float, bottom: Float, clippingBounds: Rect): Int {
    val topVisible = clippingBounds.containsInclusive(x, top)
    val bottomVisible = clippingBounds.containsInclusive(x, bottom)
    var flags = 0
    if (topVisible || bottomVisible) flags = flags or CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION
    if (!topVisible || !bottomVisible) flags = flags or CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION
    return flags
}

private fun visibilityFlags(bounds: Rect, clippingBounds: Rect?): Int {
    if (clippingBounds == null) return CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION
    var flags = 0
    if (clippingBounds.overlaps(bounds)) flags = flags or CursorAnchorInfo.FLAG_HAS_VISIBLE_REGION
    if (!clippingBounds.containsInclusive(bounds.left, bounds.top) ||
        !clippingBounds.containsInclusive(bounds.right, bounds.bottom)
    ) {
        flags = flags or CursorAnchorInfo.FLAG_HAS_INVISIBLE_REGION
    }
    return flags
}

private fun Rect.containsInclusive(x: Float, y: Float): Boolean = x in left..right && y in top..bottom

private fun ComposeMatrix.toAndroidMatrix() = Matrix().also { result ->
    val source = values
    result.setValues(
        floatArrayOf(
            source[0], source[4], source[12],
            source[1], source[5], source[13],
            source[3], source[7], source[15],
        )
    )
}

private fun View.calculateLocalToScreenMatrix(result: Matrix) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        HiroViewMatrixApi29.transformToGlobal(this, result)
        return
    }
    result.reset()
    transformToScreenBeforeApi29(result)
}

private fun View.transformToScreenBeforeApi29(result: Matrix) {
    val parentView = parent as? View
    if (parentView != null) {
        parentView.transformToScreenBeforeApi29(result)
        result.preTranslate(-parentView.scrollX.toFloat(), -parentView.scrollY.toFloat())
    } else {
        val screen = IntArray(2).also(::getLocationOnScreen)
        val window = IntArray(2).also(::getLocationInWindow)
        result.preTranslate((screen[0] - window[0]).toFloat(), (screen[1] - window[1]).toFloat())
    }
    result.preTranslate(left.toFloat(), top.toFloat())
    if (!matrix.isIdentity) result.preConcat(matrix)
}

@RequiresApi(29)
private object HiroViewMatrixApi29 {
    @JvmStatic
    fun transformToGlobal(view: View, matrix: Matrix) = view.transformMatrixToGlobal(matrix)
}

@RequiresApi(33)
private object HiroCursorAnchorInfoApi33 {
    @JvmStatic
    fun setEditorBounds(builder: CursorAnchorInfo.Builder, bounds: Rect) {
        val androidBounds = RectF(bounds.left, bounds.top, bounds.right, bounds.bottom)
        builder.setEditorBoundsInfo(
            EditorBoundsInfo.Builder()
                .setEditorBounds(androidBounds)
                .setHandwritingBounds(androidBounds)
                .build()
        )
    }
}

@RequiresApi(34)
private object HiroCursorAnchorInfoApi34 {
    @JvmStatic
    fun addVisibleLineBounds(
        builder: CursorAnchorInfo.Builder,
        layout: TextLayoutResult,
        clippingBounds: Rect,
    ) {
        if (clippingBounds.isEmpty || layout.lineCount == 0) return
        for (line in 0 until layout.lineCount) {
            val lineBounds = Rect(
                layout.getLineLeft(line),
                layout.getLineTop(line),
                layout.getLineRight(line),
                layout.getLineBottom(line),
            )
            if (clippingBounds.overlaps(lineBounds)) {
                builder.addVisibleLineBounds(lineBounds.left, lineBounds.top, lineBounds.right, lineBounds.bottom)
            }
        }
    }
}
