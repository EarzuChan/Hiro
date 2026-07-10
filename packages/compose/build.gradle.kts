import me.earzuchan.hiro.buildlogic.HiroBuildConfig
import me.earzuchan.hiro.buildlogic.processToHiroJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

val composeJar = processToHiroJar("compose") {
    // 版本对齐 CMP 1.11.1 大版本。坐标和版本须跟随上游 CMP 所使用的来
    artifacts(
        "androidx.compose.runtime:runtime-desktop:1.11.2",
        "androidx.compose.runtime:runtime-saveable-desktop:1.11.2",
        "androidx.compose.runtime:runtime-retain-desktop:1.11.2",
        "androidx.compose.runtime:runtime-annotation-jvm:1.11.2",
        "org.jetbrains.compose.animation:animation-core-desktop:1.11.1",
        "org.jetbrains.compose.animation:animation-desktop:1.11.1",
        "org.jetbrains.compose.foundation:foundation-desktop:1.11.1",
        "org.jetbrains.compose.foundation:foundation-layout-desktop:1.11.1",
        "org.jetbrains.compose.ui:ui-backhandler-desktop:1.11.1",
        "org.jetbrains.compose.ui:ui-desktop:1.11.1",
        "org.jetbrains.compose.ui:ui-geometry-desktop:1.11.1",
        "org.jetbrains.compose.ui:ui-graphics-desktop:1.11.1",
        "org.jetbrains.compose.ui:ui-text-desktop:1.11.1",
        "org.jetbrains.compose.ui:ui-unit-desktop:1.11.1",
        "org.jetbrains.compose.ui:ui-util-desktop:1.11.1"
    )

    // 处理

    dropPathPrefix("androidx/compose/ui/awt/", "androidx/compose/runtime/saveable/serialization/SerializableSaverKt")

    dropPathFragment(
        "Awt",
        "Swing",
        "JPopup",
        "ComposeContainer",
        "ComposeSceneMediator",
        "DesktopComposeSceneLayer",
        "WindowComposeSceneLayer",
        "DesktopUriHandler",
        "DesktopPlatformLocale",
        "DisposableSaveableStateRegistry",
        "SaveableStateRegistryWrapper",
        "androidx/compose/foundation/lazy/layout/DefaultLazyKey"
    )

    dropBinaryPattern(
        "androidx/compose/ui/platform/AndroidComposeView",
        "androidx/compose/ui/platform/AndroidOwner",
        "androidx/compose/ui/platform/AndroidUiDispatcher",
        "androidx/compose/ui/platform/RenderNodeLayer",
        "androidx/compose/ui/platform/ViewLayer",
        "androidx/compose/ui/graphics/AndroidCanvas",
        "androidx/compose/ui/graphics/AndroidPaint",
        "android/graphics/RuntimeShader",
        "android/graphics/RenderEffect",
        "java/awt/",
        "javax/swing/",
        "javafx/",
        "androidx/compose/ui/awt/",
        "org/jetbrains/skiko/awt/",
        "org/jetbrains/skiko/MainUIDispatcher",
        "androidx/compose/ui/platform/DesktopUriHandler"
    )

    // 校验

    requireJarEntry(
        "androidx/compose/ui/scene/CanvasLayersComposeScene_skikoKt.class",
        "androidx/compose/ui/scene/ComposeSceneRecomposer.class",
        "androidx/compose/ui/graphics/SkiaBackedCanvas.class",
        "androidx/compose/ui/graphics/SkiaBackedCanvas_skikoKt.class",
        "androidx/compose/ui/ComposeUiFlags_skikoKt.class",
        "androidx/compose/ui/SkikoComposeUiFlags.class",
        "androidx/compose/ui/text/intl/PlatformLocaleKt.class"
    )

    forbidJarEntryFragment(
        "AndroidComposeView",
        "android/graphics/RuntimeShader",
        "android/graphics/RenderEffect",
        "java/awt/",
        "javax/swing/",
        "javafx/",
        "androidx/compose/ui/awt/",
        "org/jetbrains/skiko/awt/",
        "androidx/compose/ui/platform/GlobalSnapshotManager_desktopKt.class",
        "androidx/compose/ui/Actuals_desktopKt.class",
        "androidx/compose/ui/platform/PlatformUriHandler_desktopKt.class",
        "androidx/compose/ui/platform/DesktopUriHandler",
        "androidx/compose/ui/text/intl/DesktopPlatformLocale",
        "androidx/compose/ui/platform/DisposableSaveableStateRegistry",
        "androidx/compose/runtime/saveable/serialization/SerializableSaverKt"
    )
}

val architectureComposeAdaptersJar = processToHiroJar("architecture-compose-adapters") {
    // 适配层保留原包名和 API，由 Hiro 统一提供；基础状态类型仍使用 Android 官方实现

    artifacts(
        "androidx.lifecycle:lifecycle-runtime-compose-desktop:2.10.0",
        "androidx.lifecycle:lifecycle-viewmodel-compose-android:2.10.0",
        "androidx.lifecycle:lifecycle-viewmodel-navigation3-android:2.10.0",
        "androidx.savedstate:savedstate-compose-desktop:1.4.0"
    )

    // 处理

    dropPathPrefix(
        "androidx/lifecycle/compose/FlowExtKt",
        "androidx/lifecycle/compose/ComposeLifecycleOwner.class",
        "androidx/lifecycle/viewmodel/compose/LocalViewModelStoreOwner_androidKt.class",
        "androidx/lifecycle/viewmodel/navigation3/ViewModelStoreNavEntryDecoratorDefaults.class"
    )

    dropBinaryPattern("androidx/activity/compose/LocalActivity")

    // 校验

    requireJarEntry(
        "androidx/lifecycle/compose/LocalLifecycleOwnerKt.class",
        "androidx/lifecycle/compose/RememberLifecycleOwnerKt.class",
        "androidx/lifecycle/viewmodel/compose/LocalViewModelStoreOwner.class",
        "androidx/lifecycle/viewmodel/compose/ViewModelKt.class",
        "androidx/lifecycle/viewmodel/navigation3/ViewModelStoreNavEntryDecorator.class",
        "androidx/savedstate/compose/LocalSavedStateRegistryOwnerKt.class",
        "META-INF/lifecycle-runtime-compose.kotlin_module",
        "META-INF/lifecycle-viewmodel-compose.kotlin_module",
        "META-INF/lifecycle-viewmodel-navigation3.kotlin_module",
        "META-INF/savedstate-compose.kotlin_module"
    )

    forbidJarEntryFragment(
        "androidx/compose/runtime/Composer.class",
        "androidx/lifecycle/compose/FlowExtKt",
        "androidx/lifecycle/compose/ComposeLifecycleOwner.class",
        "androidx/lifecycle/viewmodel/compose/LocalViewModelStoreOwner_androidKt.class",
        "androidx/lifecycle/viewmodel/navigation3/ViewModelStoreNavEntryDecoratorDefaults.class",
        "androidx/lifecycle/Lifecycle.class",
        "androidx/lifecycle/ViewModel.class",
        "androidx/lifecycle/ViewModelStore.class",
        "androidx/savedstate/SavedState.class",
        "androidx/savedstate/SavedStateRegistry.class"
    )
}

android {
    namespace = "${HiroBuildConfig.namespace}.compose"
    compileSdk = HiroBuildConfig.androidCompileSdk

    defaultConfig {
        minSdk = HiroBuildConfig.androidMinSdk
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing { singleVariant("release") { withSourcesJar() } }
}

tasks.withType<KotlinCompile>().configureEach { compilerOptions.moduleName.set("hiro-compose") }

val excludePwned = Action<ExternalModuleDependency>  {
    exclude(group = "androidx.compose.runtime")
    exclude(group = "androidx.compose.ui")
    exclude(group = "org.jetbrains.compose.runtime")

    exclude(group = "androidx.lifecycle", module = "lifecycle-runtime-compose")
    exclude(group = "androidx.lifecycle", module = "lifecycle-viewmodel-compose")
    exclude(group = "androidx.lifecycle", module = "lifecycle-viewmodel-navigation3")

    exclude(group = "androidx.savedstate", module = "savedstate-compose")
}

val excludePwnedAndJvm = Action<ExternalModuleDependency> {
    excludePwned(this)

    exclude(group = "androidx.annotation")
    exclude(group = "androidx.collection")
}

dependencies {
    api(project(":skia"))

    implementation(libs.androidx.core)
    implementation("androidx.activity:activity:1.12.4", dependencyConfiguration = excludePwned)

    // CMP 相关依赖，经处理进Jar。坐标和版本基本跟随上游 CMP 所使用的来
    api(composeJar.files)
    api(architectureComposeAdaptersJar.files)

    // 下面这些，不需要处理进Jar，一般不会导致冲突，Gradle一般能智能合并
    api("androidx.navigationevent:navigationevent:1.1.2", dependencyConfiguration = excludePwnedAndJvm)
    api("androidx.navigationevent:navigationevent-compose:1.1.2", dependencyConfiguration = excludePwnedAndJvm)
    api("androidx.annotation:annotation-jvm:1.10.0")
    api("androidx.collection:collection-jvm:1.6.0")
    runtimeOnly("org.jetbrains.kotlinx:atomicfu-jvm:0.28.0")

    // 基础状态类型由 Android 与 Hiro 共用，不属于 Hiro 接管模块
    api("androidx.savedstate:savedstate:1.4.0", dependencyConfiguration = excludePwned)
    api("androidx.lifecycle:lifecycle-runtime:2.10.0", dependencyConfiguration = excludePwned)
    api("androidx.lifecycle:lifecycle-viewmodel:2.10.0", dependencyConfiguration = excludePwned)
    api("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.10.0", dependencyConfiguration = excludePwned)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "compose"
            val publication = this
            project.afterEvaluate { publication.from(project.components["release"]) }
        }
    }
}

tasks.named("check") { dependsOn(composeJar, architectureComposeAdaptersJar) }
