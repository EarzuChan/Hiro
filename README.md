# Hiro

// Maven 吧唧稍后写

实验性的，将 Compose Multiplatform 的 Skiko 版使用在 Android 上。

好处：使得更低版本的安卓设备（API 24+）都尽可使用 GPU 渲染，与 SkSl 的 Shader，弥补低版本安卓设备没办法吃上特殊效果（如液态玻璃、华丽质感材料）的缺憾。

## 提供的玩意

- Hiro Skia：对 Skia、Skiko 的打包，在安卓上提供 Skia Layer 和 SkiaSurfaceView。这也是 Hiro Compose 的根基。
- Hiro Compose：对 Compose Multiplatform 的 UI/Runtime/Foundation 等的 Skiko/Desktop（即 Jvm，背后也是 Skiko）进行打包，并补齐了在安卓上的体验（接入各种事件，并提供 HiroComposeView等）。
- Hiro Material 3：对 Compose Multiplatform 的 Material 3 的 Skiko/Desktop（即 Jvm，背后也是 Skiko）的打包与体验补齐。
- Hiro Bundle：Facade，引入等于一次引入Skia+Compose，为用户提供便利。
- Hiro Gradle Plugin：帮助用户实现无感对第三方 Compose 包的 Skiko/Desktop 模块转用（ Android 转用 Skiko/Desktop，并阻断对 AndroidX Compose / Compose Multiplatform 的 Android 的依赖引入）。
- Examples，几个使用例：普通 Hiro Compose、Hiro Compose Material 3、Hiro Compose + 第三方包。

## 用起来

// 稍后写

## 更多

本项目采用 MIT 协议开源，若对您有帮助，请Star。
本项目在开发过程中使用了 OpenAI Codex 等人工智能辅助。
联系我：kontakt@earzuchan.me。