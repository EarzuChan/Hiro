# me.earzuchan.hiro:compose

打包经改造的 Compose Multiplatform 的 UI/Runtime/Foundation/Animation 等的 Skia 或 Desktop（即 Jvm，背后也是基于 Skia）工件，以及 Lifecycle、SavedState 的 Compose 适配层与 JetBrains Compose 资源运行库。
顶替冲突项目，补齐安卓体验（接入各种事件，并提供 HiroComposeView、HiroActivityExtension）。

注：

- `androidx/`、`org/jetbrains/compose/` 下多为顶替工作：与上游同包同名，替换已从处理后 Jar 中剥离的实现；另有少量与 AndroidX 同名的新增接口，如 `androidx.compose.ui.res` 的资源函数、`LocalContext` 与动画矢量资源。
- `me/earzuchan/hiro/` 下是 Hiro 自有实现，其中 `internal/res` 为资源解析与绘制。

## 关于测试

要小而精。以纸面（单元、冒烟、集成）测试为少，以 `Examples` 实机体验和 Debug 为真理。
