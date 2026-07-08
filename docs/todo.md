# 待办

- Edge2Edge：建议给Examples换用Ax.Activity的（但那个要求Activity用组件活动），现行的太牢了
- Compose：R和Res，ViewModel、LifeCycle、SavedState
- Sysbar适配：基本完成（但部分设备（小米15Ultra）有误差），差更好的用户自定义入口
- 测试液态玻璃和Miuix：液态已跑通，背后是修了Skiko
- Input生命周期Log
- Input手感调优：AI说可接ViewCfg里的参数
- 更多Input接入（虚拟触控板、键盘）
- IME与TextField正常性
- HGP优化：基本OK
- 性能优化，直通Skia：有意外闪退（小米15Ultra）
- 不兼容：用户无法使用AndroidxActivity，我们HGP会阻断其内在AndroidxAnnotation的引入（我们有Annotation，但版本不同），Gradle会报解析依赖失败
- 不兼容：观察到部分库引入“非1.11.1”的Compose相关依赖，Gradle会报解析这些依赖失败