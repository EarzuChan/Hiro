@file:JvmName("Actuals_desktopKt")

package androidx.compose.ui

import me.earzuchan.hiro.compose.internal.HiroAndroidUiDispatcher
import kotlin.coroutines.CoroutineContext

internal val PostDelayedDispatcher: CoroutineContext get() = HiroAndroidUiDispatcher
