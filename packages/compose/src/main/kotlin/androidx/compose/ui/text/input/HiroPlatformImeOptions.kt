package androidx.compose.ui.text.input

import androidx.compose.runtime.Immutable

/** 输入法私有选项 */
@Immutable
open class PlatformImeOptions(val privateImeOptions: String? = null) {
    override fun equals(other: Any?) = this === other || other is PlatformImeOptions && privateImeOptions == other.privateImeOptions

    override fun hashCode() = privateImeOptions.hashCode()

    override fun toString() = "【输入法选项（喜）：$privateImeOptions】"
}
