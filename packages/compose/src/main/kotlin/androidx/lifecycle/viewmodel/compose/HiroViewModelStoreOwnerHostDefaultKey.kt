@file:JvmName("LocalViewModelStoreOwner_desktopKt")

package androidx.lifecycle.viewmodel.compose

import androidx.compose.runtime.HostDefaultKey
import androidx.lifecycle.ViewModelStoreOwner

// TIPS：Skiko 版 UI 的 HostDefaultProviderImpl 按桌面版门面类取键，这里转发到安卓版工件的同一个键，二者才能判等
@get:JvmName("getViewModelStoreOwnerHostDefaultKey")
internal val hiroViewModelStoreOwnerHostDefaultKey: HostDefaultKey<ViewModelStoreOwner?> get() = ViewModelStoreOwnerHostDefaultKey
