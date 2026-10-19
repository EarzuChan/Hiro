@file:JvmName("PlatformUriHandler_desktopKt")

package androidx.compose.ui.platform

import me.earzuchan.hiro.log.HiroLog

private object HiroUriHandler : UriHandler {
    private const val TAG = "HiroPlatformUriHandler"

    override fun openUri(uri: String) {
        // TODO
        HiroLog.d(TAG) { "打开链接被调用，但暂未接入安卓意图：$uri" }
    }
}

internal fun createPlatformUriHandler(): UriHandler = HiroUriHandler
