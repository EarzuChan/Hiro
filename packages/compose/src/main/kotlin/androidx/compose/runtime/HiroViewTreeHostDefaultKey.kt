package androidx.compose.runtime

import androidx.annotation.IdRes

interface ViewTreeHostDefaultKey<T> : HostDefaultKey<T> {
    @get:IdRes val tagKey: Int
}
