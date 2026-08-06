@file:JvmName("KeyMapping_desktopKt")
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package androidx.compose.foundation.text

private var keyMappingOverride: KeyMapping? = null

internal val platformDefaultKeyMapping: KeyMapping
    get() = keyMappingOverride ?: defaultKeyMapping

internal fun getKeyMappingOverride(): KeyMapping? = keyMappingOverride

internal fun setKeyMappingOverride(value: KeyMapping?) {
    keyMappingOverride = value
}
