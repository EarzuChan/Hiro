# 待办

- Edge2Edge：建议给Examples换用Ax.Activity的，现行的太牢了
- Compose：ViewModel、LifeCycle、SavedState的支持基本完成，接下来是R和Res
- Sysbar适配：基本完成（但部分设备（小米15Ultra）有误差），差更好的用户自定义入口。Sysbar的颜色问题？（这是AndroidX的锅还是我的）
- 测试液态玻璃和Miuix：已跑通
- 内建各组件的关键点Log，以及基于Debug/Release的启用性门控
- Input手感调优：AI说可接ViewCfg里的参数，以及Overscroll得修复
- 更多Input接入（虚拟触控板、键盘）
- IME与TextField正常性
- HGP优化：基本OK。考虑是否引入剥离豁免
- BuildLogic优化：是否需要再简化代码？黑曼巴
- 考虑本机库、Compose的减小化、可被Tree-shake和架构裁剪
- 切换系统颜色导致Compose重置（可能是Activity重开）。但其实有办法让Activity不重开，想办法解决一下