import me.earzuchan.hiro.buildlogic.HiroBuildConfig
import me.earzuchan.hiro.buildlogic.hiroProcessedJar

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

val windowlessComposeJar = hiroProcessedJar("windowless-compose") {
    outputFileName = "hiro-compose-skia-windowless.jar"

    // 版本对齐 CMP 1.11.1 大版本
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

    dropPathPrefix("androidx/compose/ui/awt/")

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
        "DisposableSaveableStateRegistry"
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
        "androidx/compose/ui/platform/DisposableSaveableStateRegistry"
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

val classicExclude = Action<ExternalModuleDependency>  {
    exclude(group = "androidx.compose.runtime")
    exclude(group = "org.jetbrains.compose.runtime")
    exclude(group = "androidx.collection")
}

dependencies {
    api(project(":skia"))

    // CMP related
    api(windowlessComposeJar.files)
    api("androidx.navigationevent:navigationevent:1.0.1", dependencyConfiguration = classicExclude)
    api("androidx.savedstate:savedstate-compose:1.4.0", dependencyConfiguration = classicExclude)
    api("androidx.lifecycle:lifecycle-runtime-compose:2.9.4", dependencyConfiguration = classicExclude)
    api("androidx.lifecycle:lifecycle-viewmodel:2.9.4", dependencyConfiguration = classicExclude)
    api("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.9.4", dependencyConfiguration = classicExclude)
    api("androidx.annotation:annotation-jvm:1.10.0")
    api("androidx.collection:collection-jvm:1.6.0")
    runtimeOnly("org.jetbrains.kotlinx:atomicfu-jvm:0.28.0") // 有被CMP所依赖
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

tasks.named("check") { dependsOn(windowlessComposeJar) }
