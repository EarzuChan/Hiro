# Hiro

// Maven 吧唧稍后写

将 Compose Multiplatform 的 Skia（Skiko） 版使用在 Android 上。

## 好处
1. 使得更低版本的安卓设备（API 24+）都尽可使用 GPU 渲染，与 SkSl 的 Shader，弥补低版本安卓设备没办法吃上特殊效果（如液态玻璃、华丽质感材料）的缺憾。
2. 这使得您（样式创建者）只需要提供一套适用于 `skiko` 或 `desktop`（继承自 `skiko`）的样式（SkSl Shader等），无需另外改动您的库或提供 AndroidX（AGSL）的版本，即可通过让他们（样式使用者）在安卓上套用完整的效果。

## 提示（or 弊端）

1. 由于这走的是 Skia，打包相应的 so（动态共享库）不可避，这将导致您的安装包增大 20MB 左右。
2. 目前接入体系尚不完善，在偏僻处可能有Bug/Todo，且性能未必最优。

## 提供的玩意

- Hiro Skia：对 Skia（Skiko） 的打包，在安卓上提供 Skia Layer 和 SkiaSurfaceView。这也是 Hiro Compose 的根基。
- Hiro Compose：对 Compose Multiplatform 的 UI/Runtime/Foundation 等的 Skia（Skiko）/Desktop（即 Jvm）进行打包，并补齐了在安卓上的体验（接入各种事件，并提供 HiroComposeView等）。
- Hiro Material 3：对 Compose Multiplatform 的 Material 3 的 Skia（Skiko）/Desktop（即 Jvm）的打包与体验补齐。
- Hiro Bundle：Facade，引入等于一次引入 Skia + Compose，为用户提供便利。
- Hiro Gradle Plugin：帮助用户实现无感对第三方 Compose 包的 Skia（Skiko）/Desktop 模块转用（ Android 转用 Skia（Skiko）/Desktop，并阻断对 AndroidX Compose / Compose Multiplatform 的 Android 的依赖引入）。
- Examples，几个使用例：普通 Hiro Compose、Hiro Compose Material 3、Hiro Compose + 第三方包。

## 用起来（Get Started）

// 稍后写

## 更多

本项目采用 MIT 协议开源，若对您有帮助，请Star。
本项目在开发过程中使用了 OpenAI Codex 等人工智能辅助。
联系我：kontakt@earzuchan.me。