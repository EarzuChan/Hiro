# me.earzuchan.hiro:compose

打包经改造的 Compose Multiplatform 的 UI/Runtime/Foundation 等的 Skia 或 Desktop（即 Jvm，背后也是基于 Skia）工件。
顶替冲突项目，补齐安卓体验（接入各种事件，并提供 HiroComposeView、HiroActivityExtension）。

注：androidx.compose.ui.*/ 是我们所做的顶替工作。

## 关于测试

要小而精。以纸面（单元、冒烟、集成）测试为少，以 `Examples` 实机体验和 Debug 为真理。