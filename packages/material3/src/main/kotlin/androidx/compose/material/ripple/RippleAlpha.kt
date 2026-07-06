package androidx.compose.material.ripple

import androidx.compose.runtime.Immutable

@Immutable
class RippleAlpha(val draggedAlpha: Float, val focusedAlpha: Float, val hoveredAlpha: Float, val pressedAlpha: Float) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RippleAlpha) return false

        if (draggedAlpha != other.draggedAlpha) return false
        if (focusedAlpha != other.focusedAlpha) return false
        if (hoveredAlpha != other.hoveredAlpha) return false
        if (pressedAlpha != other.pressedAlpha) return false

        return true
    }

    override fun hashCode(): Int {
        var result = draggedAlpha.hashCode()
        result = 31 * result + focusedAlpha.hashCode()
        result = 31 * result + hoveredAlpha.hashCode()
        result = 31 * result + pressedAlpha.hashCode()
        return result
    }

    override fun toString(): String = "啊一个波纹阿尔法【拖动时：$draggedAlpha，焦点时：$focusedAlpha，悬浮时：$hoveredAlpha，按下时：$pressedAlpha】"
}
