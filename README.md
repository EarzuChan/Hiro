# Hiro

![Version](https://img.shields.io/badge/Version-1.9.0--alpha--cmp1.12.2-red?style=flat-square)
[![Maven](https://img.shields.io/badge/Maven-EarzuChan-blue?style=flat-square)](https://earzuchan.github.io/maven/)
[![License](https://img.shields.io/badge/License-MIT-purple?style=flat-square)](https://opensource.org/license/MIT)

[English](README_EN.md)

在 Android 上使用 Compose Multiplatform 的 Skia（Skiko）版本。

![Hiro](art/banner.png)

> 项目名称源自于**魔法少女ノ魔女裁判**中的角色**二階堂ヒロ**

## 好处

1. 使得更低版本的安卓设备（API 28，安卓 9）都尽可使用 GPU 渲染与 SkSl 的 Shader，弥补低版本安卓设备没办法吃上特殊效果（如液态玻璃、华丽质感材料）的缺憾。
2. 这使得您（样式创建者）只需要提供一套适用于 `skiko` 或 `desktop`（继承自 `skiko`）的样式（SkSl Shader等），无需另外改动您的库或提供 AndroidX（AGSL）的版本，即可通过让他们（样式使用者）在安卓上套用完整的效果。

## 提示（or 弊端）

1. 由于这走的是自提供的 Skia，打包相应的 `.so`（动态共享库）与相关资源不可避，这将在 `向安装包打入 x86_64、arm64-v8a 双架构本机库以及相关资源` 的情况下导致您的安装包增大 6MB 左右，我已竭尽全力减小裁剪以缩减体积。
2. 目前接入体系尚不 100% 完善，在一些极冷门的使用场景可能有 Bug/Todo，且性能未必全场景最优，详见下文[已知限制](#已知限制)。

## 提供的玩意

- **Hiro Skia**：对 Skia（Skiko）的打包，在安卓上提供 `SkiaLayer` 和 `SkiaSurfaceView`。这也是 Hiro Compose 的根基。
- **Hiro Compose**：基于 Compose Multiplatform 的 UI/Runtime/Foundation 等的 SkikoDesktop 版本以及相关组件作为底版制作，并深度修补，尽力补齐在安卓上的体验（接入各种事件，并提供 HiroComposeView等）。
- **Hiro Material 3**：对 Compose Multiplatform 的 Material 3 的 Skia（Skiko）/Desktop（即 Jvm）的打包与体验补齐。
- **Hiro Gradle Plugin**：帮助用户实现无感对第三方 Compose 包的 Skia（Skiko）/Desktop 模块转用（ Android 转用 Skia（Skiko）/Desktop，并阻断对 AndroidX Compose / Compose Multiplatform 的 Android 的依赖引入），把这些第三方包中的桌面平台类型（如 `SavedState`）重新链接到安卓，还能在纯安卓模块中代为生成 JetBrains Compose 资源（`Res`）。
- **Examples，几个使用例**：不但是我们开发测试时的参考面，也可以让您深度体验和了解 Hiro 各方面能力。
- **[Whole App Example](https://github.com/EarzuChan/HiroWholeAppExample)**：一个简单但架构完整的 App，用以展示 Hiro 在开发可用 App 上的可行性。

## 用起来（Getting Started）

1. 创建一个标准的 Android 项目（AGP 9、Kotlin 2.2 及以上，minSdk 28）。
2. 配置 Maven 仓库：在项目根目录的 **`settings.gradle.kts`** 文件中进行以下配置：
   在 `pluginManagement` 和 `dependencyResolutionManagement` 的 `repositories` 块中均添加我的仓库：
   ```kotlin
   maven("https://earzuchan.github.io/Maven/") // 注意：请确保“Maven”中的字母“M”为大写
   ```
   Skiko 已随 Hiro Skia 一同发布，无需再添加 JetBrains Compose Dev 仓库。
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
   * **Hiro Skia**：`hiro:compose` 已传递暴露其 API，使用 Hiro Compose 时无需显式再引入其依赖；仅在独立使用 Skia 时需显式引入。
   * **Hiro Material 3**：Material 3 未捆绑在 `hiro:compose` 中。如需使用它们，须添加此模块。
   ```kotlin
   // 仅在需要时添加：
   implementation("me.earzuchan.hiro:skia:<VERSION>") // 用于独立使用 Hiro Skia
   implementation("me.earzuchan.hiro:material3:<VERSION>") // 用于使用 Material 3 组件
   ```
5. 使用方法：在您的 Activity 中导入 `me.earzuchan.hiro.compose.setHiroComposeContent`，然后在 `onCreate` 中使用：
   ```kotlin
   setHiroComposeContent {
      // Compose 内容
   }
   ```

## 资源

- **原版安卓资源（R 与 Assets）**：提供与 AndroidX 同名的接口，如 `stringResource`、`pluralStringResource`、`colorResource`、`dimensionResource`、`painterResource`、`ImageVector.vectorResource`、`ImageBitmap.imageResource`，以及 `LocalContext`、`LocalConfiguration`、`LocalResources`；语言、深色模式等配置变化时自动重组。
  - `painterResource` 支持矢量图（含渐变）、位图、九宫格（.9.png）与形状（shape）；动画矢量请用 `AnimatedImageVector.animatedVectorResource` 搭配 `rememberAnimatedVectorPainter`。
  - 字体支持 `Font(R.font.xxx)` 与 `Font("fonts/xxx.ttf", assets)`。
- **JetBrains 的 Compose 资源（Res）**：纯安卓模块下，请您应用与 Hiro 所跟随版本一致的 Compose 插件，并把资源放进 `src/main/composeResources`，Hiro Gradle 插件会代为生成 `Res`（默认包名为 `<group>.<模块名>.generated.resources`，可经 `compose.resources {}` 自定义包名与类名）。该插件仅作为构建期的资源编译工具与配置入口，不会引入官方 Compose 运行库：
  ```kotlin
  plugins {
      id("org.jetbrains.compose") version "1.12.2"
  }
  ```

## 日志

Hiro 的日志在不可调试的 App（Release）中只输出警告与错误，且经 R8 压缩后连同字符串一并移除；可调试的 App 默认输出调试级别，可按需调整：

```kotlin
import me.earzuchan.hiro.log.HiroLogLevel
import me.earzuchan.hiro.log.HiroLogging

HiroLogging.level = HiroLogLevel.Verbose // 查看触摸、波纹等逐事件日志
```

## 特别说明

- 本项目的 Compose 和 Skiko 内部上游依赖会随 Compose Multiplatform（CMP）的主要稳定版本一同更新。当使用 Hiro Compose 或 Hiro Material 3 时，您无需引入官方（原始）的 Compose 依赖项（无论是 AndroidX 还是 CMP），这能确保所有 Compose 内容都基于我特定的类路径（Hiro 独家的 Android Compose Skiko）进行解析。我的 Hiro Gradle 插件（HGP）也会帮您使您和您的依赖不额外引入原版 Compose。
- Hiro Compose 的 Compose 和 Skia/GL 跑在独立渲染线程，通过状态机模型与安卓主线程往来。它补齐了基本的安卓事件输入（点击、导航对接等）、Lifecycle 和 SavedState 适配（并提供了作为添头的 `Kotlinx Serialization @Serializable` 在 SavedState 中的开箱即用支持，以及用户可选（需要用户实现接口）提供的自定义类型 SeDes（序列化反序列化）SavedState 支持）。关于 Hiro Compose 的更多，建议查看[架构说明](docs/hiro-compose-arch.md)。
- Hiro Skia 基本上是为 Hiro Compose 打造的。我没有直接依赖存在已知 Bug（比如把 `Rect.makeWH` 的参数传成了 `width, width`）的 JetBrains 官方安卓代码，而是实现了一套轻量级的 Skia Layer 和 SurfaceView，对于 Hiro Compose 的显示基本够用。
- 我根据 AOSP Material 3 水波纹动画的底层原理，使用 Skia 图形栈复刻了该效果。这使得在低版本 Android、魔改 ROM 上也能有一致的 Material 3 水波纹体验。这个复刻已内含在 Hiro Material 3 中，我或许之后会另外贡献给上游 CMP。
- 我修复了 JetBrains Skiko 原生代码中存在的 `JNI Critical Section Violation: using JNI after critical get` 问题，Hiro Skia 含有修复后的本机库。该修复的 [PR](https://github.com/JetBrains/skiko/pull/1235) 已被 Skiko 合并，待其随上游版本发布后 Hiro 将不再需要自行修补；此外，BreakIterator 的数组类型问题上游已自行修复。

## 已知限制

- **多线程与组件隔离**：
    - 由于 Compose 运行在 Hiro 独立渲染线程，`LocalContext` 等只宜用于读取资源等线程安全的操作；**需要主线程的操作（如 `Toast`）请自行切回主线程**。
    - Hiro Compose 世界中的 `ViewModelStore` 与安卓世界中的（如 Activity）不互通（这是出于性能考量的刻意设计）。
- **本机库架构与图形后端**：
    - 本机库仅含 `arm64-v8a`（Arm64）与 `x86_64`（x64）。在雷电等依赖 ARM 转译的 x86 模拟器上，或会因转译层无法加载 Skiko 本机库而崩溃（官方库同样如此），针对它们请使用 `x86_64`。
    - 出于包体积、设备兼容性与实现逻辑复杂度的考量，Skia 图形后端暂只对接 OpenGL ES 而不引入 Vulkan（要支持 Vulkan 比较麻烦，留待以后考虑）。
- **Skia 裁剪与独立使用限制**：
    - Hiro Skia 的本机库按 Compose 所需进行了裁剪构建——`Codec`、`Skottie`、`TextBlobBuilder` 等包装已移除；`TextBlob`、`TextLine`、`Pixmap`、`Drawable`、`Blender`、`Shaper` 等虽仍保留类型，但调用会抛出 `UnsatisfiedLinkError`。
    - 将 Hiro Skia 用于非 Hiro Compose 的独立 Skia 承载用途，未经深度测试。
- **暂不支持的系统特性与资源**：
    - 尚未接入无障碍（TalkBack 等读屏读不到 Hiro 内容）与拖放。
    - `painterResource` 暂不支持 `selector`、`layer-list` 等其他 XML Drawable。
    - 暂不支持 XML 字体族（含可下载字体）。

## 已知问题、工程进展

参见我的[待办项](docs/todo.md)。

## 写在最后

- 联系我：kontakt@earzuchan.me

### 本项目

- 基于 [MIT](LICENSE) 协议开源，若对您有帮助，请 Star。所含与改写的第三方作品及致谢见[第三方声明](THIRD_PARTY_NOTICES.md)。
- 在开发过程中使用了 OpenAI GPT、Anthropic Claude 等人工智能辅助。
- 是实验性质的，我不作任何生产环境完全可用性保证，请您自行评估是否取用于生产环境，使用风险需自行承担。
- 仍在持续优化中，为尽力给您提供“能用、可用”的体验。
