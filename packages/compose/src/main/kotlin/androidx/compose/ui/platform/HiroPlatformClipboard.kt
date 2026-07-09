@file:JvmName("PlatformClipboard_desktopKt")

package androidx.compose.ui.platform

import android.util.Log
import androidx.compose.ui.text.AnnotatedString

private object HiroNopNativeClipboard

// TODO：接入

@Suppress("DEPRECATION")
private object HiroClipboardManager : ClipboardManager {
    private const val TAG = "HiroPlatformClipboard"

    override fun setText(annotatedString: AnnotatedString) {
        Log.d(TAG, "剪贴板写入文本被调用，但暂未接入安卓剪贴板")
    }

    override fun getText() = null.also { Log.d(TAG, "剪贴板读取文本被调用，但暂未接入安卓剪贴板") }

    override fun hasText() = false

    override fun getClip() = null.also { Log.d(TAG, "剪贴板读取切片被调用，但暂未接入安卓剪贴板") }

    override fun setClip(clipEntry: ClipEntry?) {
        Log.d(TAG, "剪贴板写入条目被调用，但暂未接入安卓剪贴板")
    }

    override val nativeClipboard: Any get() = HiroNopNativeClipboard
}

private object HiroClipboard : Clipboard {
    private const val TAG = "HiroPlatformClipboard"

    override suspend fun getClipEntry() = null.also { Log.d(TAG, "剪贴板读取条目被调用，但暂未接入安卓剪贴板") }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        Log.d(TAG, "剪贴板写入条目被调用，但暂未接入安卓剪贴板")
    }

    override val nativeClipboard: Any get() = HiroNopNativeClipboard
}

@Suppress("DEPRECATION")
internal fun createPlatformClipboardManager(): ClipboardManager = HiroClipboardManager

internal fun createPlatformClipboard(): Clipboard = HiroClipboard
