@file:JvmName("LocalNavigationEventDispatcherOwner_nonAndroidKt")

package androidx.navigationevent.compose

import androidx.compose.runtime.HostDefaultKey
import androidx.navigationevent.NavigationEventDispatcherOwner

// TIPS：Skiko 版 UI 的 HostDefaultProviderImpl 按非安卓门面类取键，这里转发到安卓版工件的同一个键，二者才能判等
@get:JvmName("getNavigationEventDispatcherOwnerHostDefaultKey")
internal val hiroNavigationEventDispatcherOwnerHostDefaultKey: HostDefaultKey<NavigationEventDispatcherOwner?> get() = NavigationEventDispatcherOwnerHostDefaultKey
