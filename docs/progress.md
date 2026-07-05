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
   - 新增`HiroBuildConfig`全局配置单例，集中保存根命名空间、Hiro/CMP 版本、Skiko 版本、AGP/Kotlin 版本、compileSdk 和 minSdk
   - Java 版本恢复为各脚本里的`JavaVersion.VERSION_11`/`JvmTarget.JVM_11`硬编码，不放进`HiroBuildConfig`
   - `targetSdk`不再放进`HiroBuildConfig`，示例应用统一复用`compileSdk`作为`targetSdk`
   - 主工程和各模块脚本改回显式配置，`namespace`在具体`build.gradle.kts`里用`${HiroBuildConfig.rootNamespace}`直接拼接，保证目的地可读
   - 根工程的`hiroBuildAll`、`hiroPublishLocal`任务显式写在根`build.gradle.kts`，公开发布模块列表不放进`HiroBuildConfig`
   - 删除`gradle/libs.versions.toml`，避免版本来源双轨化
   - `:skiko`的 Skiko Android native runtime 解包任务和检查任务类型已迁移到`build-logic`，具体配置仍显式写在`:skiko`的`build.gradle.kts`
   - `HiroEglConfigChooser`已接管 EGL config 选择，`red/green/blue/alpha/depth/stencil/sampleCount`会真正参与 EGL 配置选择
   - Skia `BackendRenderTarget`创建时改用当前 GL framebuffer 实际的 sample count 和 stencil bits，并以配置值作为兜底
   - 在渲染调度、Picture 录制、vsync 背压附近补充中文`TODO`，标出 P0.3 前后需要进一步优化的位置
   - 已执行并通过：`:skiko:hiroCheckSkikoAndroidJniLibs :samples:fullscreen:assembleDebug、hiroBuildAll hiroPublishLocal`
