# Hiro

![Version](https://img.shields.io/badge/Version-1.2.3--alpha--cmp1.11.1-red?style=flat-square)
[![Maven](https://img.shields.io/badge/Maven-EarzuChan-blue?style=flat-square)](https://earzuchan.github.io/maven/)
[![License](https://img.shields.io/badge/License-MIT-purple?style=flat-square)](https://opensource.org/license/MIT)

[English](README_EN.md)

在 Android 上使用 Compose Multiplatform 的 Skia（Skiko）版本。

![Hiro](art/banner.png)

> 项目名称源自于**魔法少女ノ魔女裁判**中的角色**二階堂ヒロ**

## 好处

1. 使得更低版本的安卓设备（API 24+）都尽可使用 GPU 渲染与 SkSl 的 Shader，弥补低版本安卓设备没办法吃上特殊效果（如液态玻璃、华丽质感材料）的缺憾。
2. 这使得您（样式创建者）只需要提供一套适用于 `skiko` 或 `desktop`（继承自 `skiko`）的样式（SkSl Shader等），无需另外改动您的库或提供 AndroidX（AGSL）的版本，即可通过让他们（样式使用者）在安卓上套用完整的效果。

## 提示（or 弊端）

1. 由于这走的是 Skia，打包相应的 `.so`（动态共享库）不可避，这将导致您的安装包增大 20MB 左右。
2. 目前接入体系尚不完善，在一些较冷门的使用场景可能有 Bug/Todo，且性能未必最优。

## 提供的玩意

- Hiro Skia：对 Skia（Skiko）的打包，在安卓上提供 `SkiaLayer` 和 `SkiaSurfaceView`。这也是 Hiro Compose 的根基。
- Hiro Compose：对 Compose Multiplatform 的 UI/Runtime/Foundation 等的 Skia（Skiko）/Desktop（即 Jvm）进行打包，并补齐了在安卓上的体验（接入各种事件，并提供 HiroComposeView等）。
- Hiro Material 3：对 Compose Multiplatform 的 Material 3 的 Skia（Skiko）/Desktop（即 Jvm）的打包与体验补齐。
- Hiro Gradle Plugin：帮助用户实现无感对第三方 Compose 包的 Skia（Skiko）/Desktop 模块转用（ Android 转用 Skia（Skiko）/Desktop，并阻断对 AndroidX Compose / Compose Multiplatform 的 Android 的依赖引入）。
- Examples，几个使用例：普通 Hiro Compose、Hiro Compose Material 3、Hiro Compose + 第三方包。

## 用起来（Get Started）

1. 创建一个标准的 Android 项目。
2. 配置 Maven 仓库：在项目根目录的 **`settings.gradle.kts`** 文件中进行以下配置：
   在 `pluginManagement` 和 `dependencyResolutionManagement` 的 `repositories` 块中均添加我的仓库：
   ```kotlin
   maven("https://earzuchan.github.io/Maven/") // 注意：请确保“Maven”中的字母“M”为大写
   ```
   在 `dependencyResolutionManagement` 的 `repositories` 块中再添加 JetBrains Compose Dev 的仓库：
   ```kotlin
   maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
   ```
3. 应用 Gradle 插件：在根目录 `build.gradle.kts` 文件的 `plugins` 块中添加：
   ```kotlin
   id("me.earzuchan.hiro") version "<VERSION>" apply false
   ```
   在您的 Android 模块的 `build.gradle.kts` 文件的 `plugins` 块中添加：
   ```kotlin
   id("me.earzuchan.hiro")
   ```
4. 添加依赖：在您的 Android 模块的 `build.gradle.kts` 文件的 `dependencies` 块中添加：
   ```kotlin
   implementation("me.earzuchan.hiro:compose:<VERSION>")
   ```
   *关于其他模块的说明：*
   * Hiro Skia：`hiro:compose` 内部使用了 `hiro:skia`，但未将其 API 显式暴露给您的项目。如需直接访问 Skia API（高级用法），须显式添加该依赖。
   * Hiro Material 3：Material 3 组件未捆绑在 `hiro:compose` 中。如需使用它们，须添加此模块。
   ```kotlin
   // 仅在需要时添加：
   implementation("me.earzuchan.hiro:skia:<VERSION>") // 用于直接调用 Hiro Skia API
   implementation("me.earzuchan.hiro:material-3:<VERSION>") // 用于使用 Material 3 组件
   ```
5. 使用方法：在您的 Activity 中导入 `me.earzuchan.hiro.compose.setHiroComposeContent`，然后在 `onCreate` 中使用：
   ```kotlin
   setHiroComposeContent {
      // Compose 内容
   }
   ```

## 说明

- 上游更新与依赖关系：本项目的 Compose 和 Skiko 内部上游依赖会随 Compose Multiplatform（CMP）的主要稳定版本一同更新。此外，当使用 Hiro Compose 或 Hiro Material 3 时，您无需引入官方（原始）的Compose 依赖项（无论是 AndroidX 还是 CMP）。这能确保所有 Compose 内容都基于我们特定的类路径（Hiro 独家的 Android Compose Skiko）进行解析。我们的 Hiro Gradle 插件（HGP）也会帮您使您和您的依赖不额外引入原版 Compose。
- Material 3 水波纹复刻：我们根据 AOSP Material 3 水波纹动画的底层原理，为 Compose Skiko 复刻该效果。这使得在低版本 Android、魔改 ROM 上也能有一致的 Material 3 水波纹体验。这个复刻已内含在 Hiro Material 3 中，我们或许之后会另外贡献给上游CMP
- JNI 修复：我们修复了 JetBrains Skiko Android 原生库中存在的 `JNI Critical Section Violation: using JNI after critical get` 问题。Hiro Skia 也已含有我们修复后构建的本机库（x64 和 Arm64）。我们或许之后会另外贡献给上游仓库
- Hiro Skia 的局限性：Hiro Skia 基本上是为 Hiro Compose 打造的。我们没有直接依赖存在已知 Bug 的 JetBrains 官方 Android 示例，而是实现了一套轻量级的 Skia 的 Layer 和 SurfaceView，对于 Hiro Compose 的显示是基本够用，而独立的 Skia 承载用途，则未经深度测试
   
## 已知问题、工程进展

参见我的[待办项](docs/todo.md)。

## 写在最后

- 联系我：kontakt@earzuchan.me

### 本项目

- 基于 MIT 协议开源，若对您有帮助，请 Star
- 在开发过程中使用了 OpenAI Codex 等人工智能辅助
- 是实验性质的，我不作任何生产环境可用性保证，请您自行评估是否取用于生产环境，使用风险需自行承担
- 仍在持续优化中，为尽力给您提供“能用、可用”的体验