@file:JvmName("Actuals_desktopKt")

package androidx.compose.ui

import me.earzuchan.hiro.compose.internal.HiroRenderDispatcherRegistry
import me.earzuchan.hiro.compose.internal.HiroSnapshotApplyDispatcher
import kotlin.coroutines.CoroutineContext

internal val PostDelayedDispatcher: CoroutineContext get() = HiroRenderDispatcherRegistry.currentDispatcher() ?: HiroSnapshotApplyDispatcher
