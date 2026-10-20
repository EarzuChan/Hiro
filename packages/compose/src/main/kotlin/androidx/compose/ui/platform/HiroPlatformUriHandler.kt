@file:JvmName("PlatformUriHandler_desktopKt")

package androidx.compose.ui.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import me.earzuchan.hiro.compose.internal.HiroAndroidApplication
import me.earzuchan.hiro.compose.internal.HiroRenderDispatcherRegistry

// TIPS：与 AndroidX AndroidUriHandler 一致，以 ACTION_VIEW 打开；优先用当前场景所在 View 的 Context，使新页面留在宿主任务栈中
private object HiroUriHandler : UriHandler {
    override fun openUri(uri: String) {
        val context = HiroRenderDispatcherRegistry.currentAndroidPlatformServices()?.view?.context ?: HiroAndroidApplication.context
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply { if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        try {
            context.startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            throw IllegalArgumentException("无法打开链接：$uri", exception)
        }
    }
}

internal fun createPlatformUriHandler(): UriHandler = HiroUriHandler
