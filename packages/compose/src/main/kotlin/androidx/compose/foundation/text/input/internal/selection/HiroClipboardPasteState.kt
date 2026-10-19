package androidx.compose.foundation.text.input.internal.selection

import android.content.ClipboardManager as AndroidClipboardManager
import androidx.compose.ui.platform.Clipboard
import me.earzuchan.hiro.log.HiroLog

internal class ClipboardPasteState(private val clipboard: Clipboard) {
    private var clipAvailable = false
    private var textAvailable = false

    val hasText: Boolean get() = textAvailable

    val hasClip: Boolean get() = clipAvailable

    suspend fun update() {
        val manager = clipboard.nativeClipboard as? AndroidClipboardManager
        try {
            clipAvailable = manager?.hasPrimaryClip() == true
            textAvailable = clipAvailable && manager?.primaryClipDescription?.hasMimeType("text/*") == true
        } catch (exception: SecurityException) {
            clipAvailable = false
            textAvailable = false
            HiroLog.w("HiroClipboardPaste", exception) { "系统拒绝了剪贴板元数据读取" }
        }
    }
}
