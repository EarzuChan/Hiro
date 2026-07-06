@file:JvmName("PlatformClipboard_desktopKt")

package androidx.compose.ui.platform

import androidx.compose.ui.text.AnnotatedString

private object HiroNoopNativeClipboard

@Suppress("DEPRECATION")
private object HiroClipboardManager : ClipboardManager {
    override fun setText(annotatedString: AnnotatedString) = Unit // TODO：接入 Android ClipboardManager

    override fun getText() = null// TODO：接入 Android ClipboardManager

    override fun hasText() = false

    override fun getClip(): ClipEntry? = null

    override fun setClip(clipEntry: ClipEntry?) = Unit // TODO：接入 Android ClipboardManager

    override val nativeClipboard: Any get() = HiroNoopNativeClipboard
}

private object HiroClipboard : Clipboard {
    override suspend fun getClipEntry(): ClipEntry? = null

    override suspend fun setClipEntry(clipEntry: ClipEntry?) = Unit // TODO：接入 Android ClipboardManager

    override val nativeClipboard: Any get() = HiroNoopNativeClipboard
}

@Suppress("DEPRECATION")
internal fun createPlatformClipboardManager(): ClipboardManager = HiroClipboardManager

internal fun createPlatformClipboard(): Clipboard = HiroClipboard
