@file:JvmName("GlobalSnapshotManager_desktopKt")

package androidx.compose.ui.platform

import kotlinx.coroutines.CoroutineDispatcher
import me.earzuchan.hiro.compose.internal.HiroSnapshotApplyDispatcher

internal val GlobalSnapshotManagerDispatcher: CoroutineDispatcher get() = HiroSnapshotApplyDispatcher
