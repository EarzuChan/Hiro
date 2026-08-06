@file:JvmName("TransferableContent_skikoKt")

package androidx.compose.foundation.content

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.ClipEntry

@ExperimentalFoundationApi
fun TransferableContent.hasMediaType(mediaType: MediaType): Boolean =
    clipMetadata.clipDescription.hasMimeType(mediaType.representation)

internal fun ClipEntry.readPlainText(): String? {
    val data = clipData
    if ((0 until data.itemCount).none { data.getItemAt(it).text != null }) return null
    return buildString {
        var hasPrevious = false
        for (index in 0 until data.itemCount) {
            data.getItemAt(index).text?.let { text ->
                if (hasPrevious) append('\n')
                append(text)
                hasPrevious = true
            }
        }
    }
}
