@file:JvmName("DesktopPlatformLocale_desktopKt")
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
// ↑：这得给我面子啊，必须好使

package androidx.compose.ui.text.intl

import android.text.TextUtils
import android.view.View
import java.util.Locale as JavaLocale

internal fun createPlatformLocaleDelegate(): PlatformLocaleDelegate = object : PlatformLocaleDelegate {
    override val current: LocaleList get() = LocaleList(listOf(Locale(JavaLocale.getDefault())))
}

internal fun Locale.isRtl(): Boolean = TextUtils.getLayoutDirectionFromLocale(platformLocale) == View.LAYOUT_DIRECTION_RTL
