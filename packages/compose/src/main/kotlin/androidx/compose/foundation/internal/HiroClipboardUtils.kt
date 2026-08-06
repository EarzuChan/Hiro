@file:JvmName("ClipboardUtils_desktopKt")

package androidx.compose.foundation.internal

import android.content.ClipboardManager as AndroidClipboardManager
import android.util.Log
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.HiroAnnotatedStringClipboardCodec
import androidx.compose.ui.text.AnnotatedString

internal suspend fun ClipEntry.readText(): String? =
    clipData.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()

internal suspend fun ClipEntry.readAnnotatedString(): AnnotatedString? =
    HiroAnnotatedStringClipboardCodec.decode(clipData.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text)

internal fun AnnotatedString?.toClipEntry(): ClipEntry? =
    this?.let { ClipEntry(android.content.ClipData.newPlainText("Hiro 文本", HiroAnnotatedStringClipboardCodec.encode(it))) }

internal fun ClipEntry?.hasText(): Boolean =
    this?.clipData?.description?.hasMimeType("text/*") == true

internal fun Clipboard.isReadSupported(): Boolean = true

internal fun Clipboard.isWriteSupported(): Boolean = true

internal fun Clipboard.nativeClipboardHasText(): Boolean {
    val manager = nativeClipboard as? AndroidClipboardManager ?: return false
    return try {
        manager.hasPrimaryClip() && manager.primaryClipDescription?.hasMimeType("text/*") == true
    } catch (exception: SecurityException) {
        Log.w("HiroClipboardUtils", "系统拒绝了剪贴板元数据读取", exception)
        false
    }
}
