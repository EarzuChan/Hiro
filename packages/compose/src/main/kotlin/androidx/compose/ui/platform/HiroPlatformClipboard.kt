@file:JvmName("PlatformClipboard_desktopKt")
@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.platform

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager as AndroidClipboardManager
import android.util.Log
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.AnnotatedString
import me.earzuchan.hiro.compose.internal.HiroRenderDispatcherRegistry

class ClipEntry @ExperimentalComposeUiApi constructor(
    @property:ExperimentalComposeUiApi val nativeClipEntry: Any,
) {
    constructor(clipData: ClipData) : this(clipData as Any)

    val clipData: ClipData
        get() = nativeClipEntry as? ClipData
            ?: error("Hiro Android ClipEntry 不包含 ClipData")

    val clipMetadata: ClipMetadata
        get() = ClipMetadata(clipData.description)
}

class ClipMetadata(val clipDescription: ClipDescription)

fun ClipData.toClipEntry(): ClipEntry = ClipEntry(this)

fun ClipDescription.toClipMetadata(): ClipMetadata = ClipMetadata(this)

private class HiroAndroidClipboardAccess(
    private val context: android.content.Context,
    val native: AndroidClipboardManager,
) {
    fun getClip(): ClipEntry? = try {
        native.primaryClip?.toClipEntry()
    } catch (exception: SecurityException) {
        Log.w(TAG, "系统拒绝了剪贴板读取", exception)
        null
    }

    fun setClip(entry: ClipEntry?) {
        try {
            if (entry == null) native.clearPrimaryClip() else native.setPrimaryClip(entry.clipData)
        } catch (exception: SecurityException) {
            Log.w(TAG, "系统拒绝了剪贴板写入", exception)
        }
    }

    fun getText(): AnnotatedString? {
        val item = getClip()?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0) ?: return null
        return HiroAnnotatedStringClipboardCodec.decode(item.coerceToStyledText(context))
    }

    fun hasText(): Boolean = try {
        native.hasPrimaryClip() && native.primaryClipDescription?.hasMimeType("text/*") == true
    } catch (exception: SecurityException) {
        Log.w(TAG, "系统拒绝了剪贴板元数据读取", exception)
        false
    }

    companion object {
        private const val TAG = "HiroPlatformClipboard"
    }
}

@Suppress("DEPRECATION")
private class HiroClipboardManager(private val access: HiroAndroidClipboardAccess) : ClipboardManager {
    override fun setText(annotatedString: AnnotatedString) {
        access.setClip(ClipEntry(ClipData.newPlainText("Hiro 文本", HiroAnnotatedStringClipboardCodec.encode(annotatedString))))
    }

    override fun getText(): AnnotatedString? = access.getText()

    override fun hasText(): Boolean = access.hasText()

    override fun getClip(): ClipEntry? = access.getClip()

    override fun setClip(clipEntry: ClipEntry?) = access.setClip(clipEntry)

    override val nativeClipboard: Any get() = access.native
}

private class HiroClipboard(private val access: HiroAndroidClipboardAccess) : Clipboard {
    override suspend fun getClipEntry(): ClipEntry? = access.getClip()

    override suspend fun setClipEntry(clipEntry: ClipEntry?) = access.setClip(clipEntry)

    override val nativeClipboard: Any get() = access.native
}

private fun createAccess(): HiroAndroidClipboardAccess {
    val services = checkNotNull(HiroRenderDispatcherRegistry.currentAndroidPlatformServices()) {
        "Hiro 剪贴板只能在已注册的渲染场景中创建"
    }
    val manager = services.applicationContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as AndroidClipboardManager
    return HiroAndroidClipboardAccess(services.applicationContext, manager)
}

@Suppress("DEPRECATION")
internal fun createPlatformClipboardManager(): ClipboardManager = HiroClipboardManager(createAccess())

internal fun createPlatformClipboard(): Clipboard = HiroClipboard(createAccess())
