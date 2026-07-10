package androidx.lifecycle.viewmodel.navigation3

import androidx.compose.runtime.Composable

@Deprecated("该兼容入口由 Hiro 提供，Navigation3 已不再依赖配置变更判断")
object ViewModelStoreNavEntryDecoratorDefaults {
    @Composable
    fun removeViewModelStoreOnPop() = { true }
}
