# 待办

- Compose：ViewModel、LifeCycle、SavedState的支持基本完成，接下来是R和Res
- 客制化：各方面的`更好的用户自定义入口`
- Sysbar适配：基本完成。Sysbar的颜色问题？（这是AndroidX的锅还是我的）
- 测试液态玻璃和Miuix：已跑通
- 内建各组件的关键点Log，以及基于Debug/Release的启用性门控
- Input手感调优：AI说可接ViewCfg里的参数，有必要吗
- 更多Input接入（虚拟触控板、键盘）
- IME与TextField正常性
- HGP优化：基本OK。考虑是否引入剥离豁免
- BuildLogic优化：是否需要再简化代码？黑曼巴
- 考虑本机库、Compose的减小化、可被Tree-shake和架构裁剪
- 切换系统颜色导致Compose重置（可能是Activity重开）。但其实有办法让Activity不重开，想办法解决一下
- 安卓7没正式试过：太老了，模拟器都与我HyperV不相容。只能说是理论支持