# 待办

- IME与TextField正常性
- Compose：ViewModel、LifeCycle、SavedState的支持基本完成，接下来是R和Res
- 客制化：各方面的`更好的用户自定义入口`：基本，HiroCfg
- Sysbar适配：基本完成。Sysbar的颜色问题？（这是AndroidX的锅还是我的）。Insets 未来可看是否做 ViewPort Relative NEWAPI
- 测试液态玻璃和Miuix：已跑通
- 内建各组件的关键点Log，以及基于Debug/Release的启用性门控
- Input手感调优：AI说可接ViewCfg里的参数，有必要吗
- 更多Input接入（虚拟触控板、键盘）
- HGP优化：基本OK。考虑是否引入剥离豁免
- BuildLogic优化：是否需要再简化代码？黑曼巴
- 考虑本机库、Compose的减小化、可被Tree-shake和架构裁剪：目前已基本减小化
- 切换系统颜色导致Compose重置（可能是Activity重开）。但其实有办法让Activity不重开，想办法解决一下：这个是具体App的Change问题
- Skiko 可以变成 Processed Jar，且这同CMP都可以再屏蔽些类
- 重建活动，Activity VM 实例能做到不变，而 Hiro VM 实例改变。这虽是预期行为，但有没有要改的

# 修复
- SelectionContainer 未顶替 Hiro ClipboardUtils_desktopKt