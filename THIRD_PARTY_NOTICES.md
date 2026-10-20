# 第三方声明

Hiro 自身以 [MIT](LICENSE) 协议开源。Hiro 发布的工件中包含、改写或链接了以下第三方作品，它们各自的许可证原文见 [licenses](licenses) 目录。在此向这些项目的作者与贡献者致以由衷的谢意，没有你们就没有 Hiro。

## 打包进 Hiro 工件的作品

| 作品 | 所在工件 | 许可证 | 说明                                                                           |
|---|---|---|------------------------------------------------------------------------------|
| [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform-core)（Runtime、UI、Foundation、Animation、Material3、Components Resources） | `compose`、`material3` | Apache-2.0 | 经处理：剔除 AWT、Swing 等桌面实现，并以 Hiro 的同包同名实现顶替部分类                                  |
| [AndroidX](https://android.googlesource.com/platform/frameworks/support)（Compose Runtime，Lifecycle、SavedState 的 Compose 适配层） | `compose` | Apache-2.0 | 经处理，方式同上                                                                     |
| [Skiko](https://github.com/JetBrains/skiko) | `skia` | Apache-2.0，附 [NOTICE](licenses/Skiko-NOTICE.txt) | Java 层经处理剔除未使用的包装；本机库由 Hiro 修补、裁剪后重新构建。Skia 带来的相关作品是因为原版 Skia 就带有，非刻意额外引入，下同 |
| [Skia](https://skia.org) | `skia`（本机库） | BSD-3-Clause | 以最小化配置构建                                                                     |
| [ICU](https://icu.unicode.org) | `skia`（本机库与 ICU 数据） | Unicode-3.0 | ICU 数据经裁剪                                                                    |
| [FreeType](https://freetype.org) | `skia`（本机库） | FreeType License（FTL） | 依 FTL 选择此许可证                                                                 |
| [HarfBuzz](https://harfbuzz.github.io) | `skia`（本机库） | MIT（Old MIT） |                                                                              |
| [libpng](http://www.libpng.org) | `skia`（本机库） | libpng |                                                                              |
| [libjpeg-turbo](https://libjpeg-turbo.org) | `skia`（本机库） | IJG、BSD-3-Clause、zlib |                                                                              |
| [libwebp](https://chromium.googlesource.com/webm/libwebp) | `skia`（本机库） | BSD-3-Clause，附专利授权 |                                                                              |
| [Expat](https://libexpat.github.io) | `skia`（本机库） | MIT |                                                                              |
| [zlib](https://zlib.net) | `skia`（本机库） | zlib |                                                                              |

## 改写自上游的源码

- Hiro 的部分同包同名顶替文件改写自 Compose Multiplatform 与 AndroidX 的对应源码（Apache-2.0），例如字体加载器、资源读取器、键盘与剪贴板适配等。
- 安卓矢量图与动画矢量的解析器移植自 AndroidX Compose UI 与 Animation Graphics（Apache-2.0），保留了原版权声明。
- Hiro Material3 的水波纹参考了 [AOSP](https://source.android.com)（Apache-2.0）Material 水波纹的实现原理。

## 仅出现在示例中的作品（用于演示目的）

- 字体 [Dancing Script](https://github.com/googlefonts/DancingScript) 与 [Cutive Mono](https://github.com/googlefonts/cutivemono)：SIL Open Font License 1.1，许可证原文随字体放在对应示例目录中。

## 致谢

- 感谢 JetBrains 与 Google 的 Compose、Skiko、AndroidX 团队，Hiro 站在你们的肩膀上。
- 感谢 [Miuix](https://github.com/compose-miuix-ui/miuix) 与 [Kyant Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) 等跨平台组件库，它们是 Hiro 第三方兼容性的试金石。