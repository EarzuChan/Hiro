# 这里记录工作进展

1. 已敲定工程结构：以`me.earzuchan.hiro`为根命名空间，提供`skiko`、`compose`、`material3`、`hiro-gradle-plugin`这些包。Maven 发版版本号和 CMP 依赖版本跟随 CMP Stable，目前从`1.11.1`开始
2. 已创建 Gradle 项目骨架和发布、插件占位：
   - 根工程`Hiro`，版本从`1.11.1`起，Maven group 为`me.earzuchan.hiro`
   - 公开模块骨架：`:skiko`、`:compose`、`:material3`、`:hiro`、`:hiro-gradle-plugin`
   - `:compose`预留 Compose 聚合与`HiroComposeView`、`HiroActivity`、宿主胶水代码的位置
   - `:hiro`仅作为`skiko + compose`聚合模块；`material3`单独发布；Legacy Material 不进入骨架
   - 创建示例占位：`:samples:fullscreen`、`:samples:material3-sample`、`:samples:third-party-libs`
   - 创建`third_party/`、`patches/`占位，用于后续接 CMP/Skiko 源码和补丁队列；`tools/`已删除，构建和发布统一走 Gradle 任务
3. 已把构建基线切到 Gradle 9 与 AGP`9.0.1`：
   - 普通 Android 库和示例应用不再应用`org.jetbrains.kotlin.android`，改用 AGP 9 内建 Kotlin
   - Android 模块保留`compileOptions`，不再重复设置 Kotlin `jvmTarget`，因为内建 Kotlin 默认跟随`android.compileOptions.targetCompatibility`
4. 已验证 Gradle 任务：使用 Gradle`9.5.1`和 AGP`9.0.1`执行`hiroBuildAll hiroPublishLocal`，结果通过
5. 已完成 P0.1：Skiko Android 后端加固与 native runtime 打包验证：
   - 新增`:skiko`里的`HiroSkiaLayer`、`HiroSkikoSurfaceView`、`HiroSkikoLayerConfig`，先提供纯 Skia 全屏渲染宿主
   - 修复参考实现里录制边界的`width,width`问题，改为使用`width,height`
   - `SurfaceView`默认使用`MATCH_PARENT`，默认使用 GLES 3，并开放 EGL、采样、模板缓冲等配置
   - 暴露`surfaceView`、`configureSurfaceView`、`onHostPause`、`onHostResume`等入口，方便后续`HiroComposeView`接生命周期
   - GL 资源释放改为通过`GLSurfaceView.queueEvent`进入 GL 线程，避免主线程直接关闭 Skia GPU 资源
   - `:skiko`不再把`skiko-android-runtime-*`作为传递运行时依赖，而是用 Gradle 任务解包到 AGP 9 的 generated `jniLibs`目录，让发布 AAR 自带`jni/arm64-v8a/libskiko-android-arm64.so`和`jni/x86_64/libskiko-android-x64.so`
   - 新增`:skiko:hiroCheckSkikoAndroidJniLibs`，用于检查 native runtime 是否已按 Android ABI 目录展开
   - 新增`:samples:fullscreen`的 P0.1 纯 Skia 示例 Activity，可用于验证 Skiko Android 全屏绘制、动画和生命周期
   - 已验证`jar tf samples/fullscreen/build/outputs/apk/debug/fullscreen-debug.apk`和 release APK 均包含`lib/arm64-v8a/libskiko-android-arm64.so`、`lib/x86_64/libskiko-android-x64.so`
   - 已验证发布到 Maven Local 的`me.earzuchan.hiro:skiko:1.11.1` AAR 包含上述 native 库
   - 已执行并通过：`:skiko:hiroCheckSkikoAndroidJniLibs :samples:fullscreen:assembleDebug hiroBuildAll hiroPublishLocal`
6. 已完成工程骨架加固：
   - 新增`build-logic`，职责收窄为提供全局配置单例、Skiko native runtime 相关任务类型，以及一个极薄 marker plugin；不再把每个子项目的 Android 配置藏进 convention plugin
   - 新增`HiroBuildConfig`全局配置单例，集中保存根命名空间、Hiro/CMP 发版版本、compileSdk 和 minSdk
   - Java 版本恢复为各脚本里的`JavaVersion.VERSION_11`/`JvmTarget.JVM_11`硬编码，不放进`HiroBuildConfig`
   - `targetSdk`不再放进`HiroBuildConfig`，示例应用统一复用`compileSdk`作为`targetSdk`
   - 主工程和各模块脚本改回显式配置，`namespace`在具体`build.gradle.kts`里用`${HiroBuildConfig.namespace}`直接拼接，保证目的地可读
   - 根工程的`hiroBuildAll`、`hiroPublishLocal`任务显式写在根`build.gradle.kts`，公开发布模块列表不放进`HiroBuildConfig`
   - 依赖坐标和外厂插件版本放在`gradle/libs.versions.toml`，Hiro 自身版本和 Android 基线仍由`HiroBuildConfig`管理
   - `:skiko`的 Skiko Android native runtime 解包任务和检查任务类型已迁移到`build-logic`，具体配置仍显式写在`:skiko`的`build.gradle.kts`
   - `HiroEglConfigChooser`已接管 EGL config 选择，`red/green/blue/alpha/depth/stencil/sampleCount`会真正参与 EGL 配置选择
   - Skia `BackendRenderTarget`创建时改用当前 GL framebuffer 实际的 sample count 和 stencil bits，并以配置值作为兜底
   - 在渲染调度、Picture 录制、vsync 背压附近补充中文`TODO`，标出 P0.3 前后需要进一步优化的位置
   - 已执行并通过：`:skiko:hiroCheckSkikoAndroidJniLibs :samples:fullscreen:assembleDebug、hiroBuildAll hiroPublishLocal`
7. 已完成 P0.2：Gradle 插件魔法最小闭环：
   - `me.earzuchan.hiro`不再是空插件，插件源码统一放在`build-logic/src/hiroGradlePlugin/kotlin`，`:hiro-gradle-plugin`复用同一份正式源码发布
   - 插件只保留`hiro.strict`配置；Skiko/JVM 变体劫持是必然行为，不提供`skikoVariantHijack`、`skikoVariantModules`之类的用户开关
   - Android main compile/runtime classpath 统一打上 Hiro 后端属性，官方 Compose 坐标在用户工程和第三方元数据里会导向`me.earzuchan.hiro:compose:1.11.1`或`me.earzuchan.hiro:material3:1.11.1`，Hiro 自身包内不做递归替换
   - `:compose`正式声明 CMP runtime/ui/foundation/animation 依赖，`:material3`正式声明 CMP Material3 依赖；Hiro 发版仍是`1.11.1`，但 CMP Material3 组件按实际发布版本使用`1.11.0-alpha07`
   - 组件元数据规则会为 CMP 模块建立 Hiro Android-Skiko 候选变体，并为第三方 KMP 库按顺序尝试`skiko`、`jvm`、`desktop`候选；`*-published`只按发布形态归并回对应候选，不作为独立语义；如果候选里绑定桌面窗口系统，strict 会失败
   - 插件会为 Android 工程排除 KMP 元数据资源进入 APK，避免`.knm`和 source set manifest 被当作 Java resource 合包；这不排除 class，也不掩盖 strict 泄漏
   - strict 默认开启，扫描最终 Android main classpath 产物里的 jar、aar 内 classes/libs 以及目录 class，发现 Android Compose 后端、Android AGSL/RenderEffect、Legacy Material、AWT/Swing/JavaFX/Skiko AWT 等既定路径即失败
   - 已执行并通过：`:build-logic:compileKotlin :hiro-gradle-plugin:compileKotlin`、`:compose:build :material3:build :hiro:build`、`:compose:hiroCheckStrictDependencies :material3:hiroCheckStrictDependencies :hiro:hiroCheckStrictDependencies :samples:fullscreen:hiroCheckStrictDependencies :samples:material3-sample:hiroCheckStrictDependencies :samples:third-party-libs:hiroCheckStrictDependencies`、`hiroBuildAll`、`hiroPublishLocal`
   - P0.3 的 Android-Skiko Compose 原型和 P0.4 的`HiroComposeView`仍未开始
