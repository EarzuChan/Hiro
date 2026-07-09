@file:JvmName("PointerIcon_desktopKt")

package androidx.compose.ui.input.pointer

// TODO：安卓端后续如需支持蓝牙鼠标、触控板、触控笔悬停，再把这些图标映射到 android.view.PointerIcon

private object HiroNopPointerIcon : PointerIcon

internal val pointerIconDefault: PointerIcon = HiroNopPointerIcon
internal val pointerIconCrosshair: PointerIcon = HiroNopPointerIcon
internal val pointerIconText: PointerIcon = HiroNopPointerIcon
internal val pointerIconHand: PointerIcon = HiroNopPointerIcon
