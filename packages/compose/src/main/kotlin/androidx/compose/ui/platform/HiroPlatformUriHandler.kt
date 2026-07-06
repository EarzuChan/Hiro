@file:JvmName("PlatformUriHandler_desktopKt")

package androidx.compose.ui.platform

private object HiroUriHandler : UriHandler {
    override fun openUri(uri: String) = Unit // TODO：接入 Android Intent 打开链接
}

internal fun createPlatformUriHandler(): UriHandler = HiroUriHandler