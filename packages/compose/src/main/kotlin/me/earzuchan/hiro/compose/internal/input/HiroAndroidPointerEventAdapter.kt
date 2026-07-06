package me.earzuchan.hiro.compose.internal.input

import android.util.SparseLongArray
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.HistoricalChange
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.scene.ComposeScenePointer

@OptIn(InternalComposeUiApi::class)
internal class HiroAndroidPointerEventAdapter {
    private val androidToComposePointerIds = SparseLongArray()
    private var nextComposePointerId = 0L

    // TODO：后续或把悬停、滚轮、触控板滚动拆成专用入口，不和普通触摸流混写
    fun convertTouchEvent(event: MotionEvent): HiroAndroidPointerDispatch? {
        val eventType = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> PointerEventType.Press

            MotionEvent.ACTION_MOVE -> PointerEventType.Move
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> PointerEventType.Release

            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_OUTSIDE -> {
                clearPointerIds()
                return HiroAndroidPointerDispatch.Cancel
            }

            else -> return null
        }

        ensurePointerIds(event)

        val releasedPointerIndex = when (event.actionMasked) {
            MotionEvent.ACTION_UP -> 0
            MotionEvent.ACTION_POINTER_UP -> event.actionIndex
            else -> -1
        }

        val pointers = List(event.pointerCount) { index ->
            val androidPointerId = event.getPointerId(index)
            ComposeScenePointer(
                id = PointerId(composePointerId(androidPointerId)),
                position = Offset(event.getX(index), event.getY(index)),
                pressed = index != releasedPointerIndex,
                type = pointerType(event, index),
                pressure = event.getPressure(index),
                historical = historicalChanges(event, index),
            )
        }

        val dispatch = HiroAndroidPointerDispatch.Event(
            HiroComposePointerEvent(
                type = eventType,
                pointers = pointers,
                buttons = pointerButtons(event),
                keyboardModifiers = pointerKeyboardModifiers(event.metaState),
                scrollDelta = Offset.Zero,
                timeMillis = event.eventTime,
                nativeEvent = event,
                changedButton = changedButton(event),
            )
        )

        when (event.actionMasked) {
            MotionEvent.ACTION_UP -> clearPointerIds()
            MotionEvent.ACTION_POINTER_UP -> androidToComposePointerIds.delete(event.getPointerId(event.actionIndex))
        }

        return dispatch
    }

    private fun ensurePointerIds(event: MotionEvent) {
        for (index in 0 until event.pointerCount) {
            val androidPointerId = event.getPointerId(index)
            if (androidToComposePointerIds.indexOfKey(androidPointerId) < 0) androidToComposePointerIds.put(androidPointerId, nextComposePointerId++)
        }
    }

    private fun composePointerId(androidPointerId: Int): Long {
        val index = androidToComposePointerIds.indexOfKey(androidPointerId)
        if (index >= 0) return androidToComposePointerIds.valueAt(index)

        val composePointerId = nextComposePointerId++
        androidToComposePointerIds.put(androidPointerId, composePointerId)
        return composePointerId
    }

    private fun clearPointerIds() {
        androidToComposePointerIds.clear()
    }

    private fun pointerType(event: MotionEvent, index: Int) = when (event.getToolType(index)) {
        MotionEvent.TOOL_TYPE_FINGER -> if (event.isFromSource(InputDevice.SOURCE_MOUSE) || event.isFromSource(InputDevice.SOURCE_TOUCHPAD)) PointerType.Mouse else PointerType.Touch
        MotionEvent.TOOL_TYPE_STYLUS -> PointerType.Stylus
        MotionEvent.TOOL_TYPE_MOUSE -> PointerType.Mouse
        MotionEvent.TOOL_TYPE_ERASER -> PointerType.Eraser
        else -> PointerType.Unknown
    }

    // TODO：后续补充触控笔倾斜、侧键和安卓高阶轴数据
    private fun historicalChanges(event: MotionEvent, index: Int) = if (event.historySize == 0) emptyList()
    else List(event.historySize) { historyIndex ->
        HistoricalChange(event.getHistoricalEventTime(historyIndex), Offset(event.getHistoricalX(index, historyIndex), event.getHistoricalY(index, historyIndex)))
    }

    private fun pointerButtons(event: MotionEvent): PointerButtons {
        val buttonState = event.buttonState

        return PointerButtons(
            isPrimaryPressed = buttonState hasButton MotionEvent.BUTTON_PRIMARY,
            isSecondaryPressed = buttonState hasButton MotionEvent.BUTTON_SECONDARY,
            isTertiaryPressed = buttonState hasButton MotionEvent.BUTTON_TERTIARY,
            isBackPressed = buttonState hasButton MotionEvent.BUTTON_BACK,
            isForwardPressed = buttonState hasButton MotionEvent.BUTTON_FORWARD,
        )
    }

    private fun changedButton(event: MotionEvent) = when (event.actionButton) {
        MotionEvent.BUTTON_PRIMARY -> PointerButton.Primary
        MotionEvent.BUTTON_SECONDARY -> PointerButton.Secondary
        MotionEvent.BUTTON_TERTIARY -> PointerButton.Tertiary
        MotionEvent.BUTTON_BACK -> PointerButton.Back
        MotionEvent.BUTTON_FORWARD -> PointerButton.Forward
        else -> null
    }

    private fun pointerKeyboardModifiers(metaState: Int) = PointerKeyboardModifiers(
        isCtrlPressed = metaState hasMeta KeyEvent.META_CTRL_ON,
        isMetaPressed = metaState hasMeta KeyEvent.META_META_ON,
        isAltPressed = metaState hasMeta KeyEvent.META_ALT_ON,
        isShiftPressed = metaState hasMeta KeyEvent.META_SHIFT_ON,
        isAltGraphPressed = false,
        isSymPressed = metaState hasMeta KeyEvent.META_SYM_ON,
        isFunctionPressed = metaState hasMeta KeyEvent.META_FUNCTION_ON,
        isCapsLockOn = metaState hasMeta KeyEvent.META_CAPS_LOCK_ON,
        isScrollLockOn = metaState hasMeta KeyEvent.META_SCROLL_LOCK_ON,
        isNumLockOn = metaState hasMeta KeyEvent.META_NUM_LOCK_ON,
    )

    private infix fun Int.hasButton(button: Int): Boolean = this and button != 0

    private infix fun Int.hasMeta(meta: Int): Boolean = this and meta != 0
}
