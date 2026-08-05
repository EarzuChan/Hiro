package androidx.compose.ui.text.input

import androidx.compose.runtime.Immutable

/** Android 平台的输入法私有选项 */
@Immutable
open class PlatformImeOptions(val privateImeOptions: String? = null) {
    override fun equals(other: Any?): Boolean = this === other || other is PlatformImeOptions && privateImeOptions == other.privateImeOptions

    override fun hashCode(): Int = privateImeOptions?.hashCode() ?: 0

    override fun toString(): String = "PlatformImeOptions(privateImeOptions=$privateImeOptions)"
}
