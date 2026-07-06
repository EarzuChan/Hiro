import me.earzuchan.hiro.buildlogic.HiroBuildConfig
import me.earzuchan.hiro.buildlogic.task.BuildWindowlessComposeJarTask
import me.earzuchan.hiro.buildlogic.task.CheckWindowlessComposeJarTask

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    id("me.earzuchan.hiro")
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

val composeSkikoArtifacts by configurations.registering {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

// TODO：呃呃，这不正规吧
val buildWindowlessComposeJar by tasks.registering(BuildWindowlessComposeJarTask::class) {
    inputJars.from(composeSkikoArtifacts)
    outputJar.set(layout.buildDirectory.file("generated/hiro/windowless-compose/hiro-compose-skiko-windowless.jar"))

    forbiddenPathPrefixes.set(listOf("androidx/compose/ui/awt/"))
    forbiddenPathFragments.set(listOf("Awt", "Swing", "JPopup", "ComposeContainer", "ComposeSceneMediator", "DesktopComposeSceneLayer", "WindowComposeSceneLayer", "DesktopUriHandler", "DesktopPlatformLocale", "DisposableSaveableStateRegistry"))
    forbiddenBinaryPatterns.set(listOf("androidx/compose/ui/platform/AndroidComposeView", "androidx/compose/ui/platform/AndroidOwner", "androidx/compose/ui/platform/AndroidUiDispatcher", "androidx/compose/ui/platform/RenderNodeLayer", "androidx/compose/ui/platform/ViewLayer", "androidx/compose/ui/graphics/AndroidCanvas", "androidx/compose/ui/graphics/AndroidPaint", "android/graphics/RuntimeShader", "android/graphics/RenderEffect", "java/awt/", "javax/swing/", "javafx/", "androidx/compose/ui/awt/", "org/jetbrains/skiko/awt/", "org/jetbrains/skiko/MainUIDispatcher", "androidx/compose/ui/platform/DesktopUriHandler"))
}

val windowlessComposeJar = files(buildWindowlessComposeJar.flatMap { it.outputJar }).builtBy(buildWindowlessComposeJar)

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

dependencies {
    add(composeSkikoArtifacts.name, libs.jetbrains.compose.animation.core.desktop)
    add(composeSkikoArtifacts.name, libs.jetbrains.compose.animation.desktop)
    add(composeSkikoArtifacts.name, libs.jetbrains.compose.foundation.desktop)
    add(composeSkikoArtifacts.name, libs.jetbrains.compose.foundation.layout.desktop)
    add(composeSkikoArtifacts.name, libs.jetbrains.compose.ui.backhandler.desktop)
    add(composeSkikoArtifacts.name, libs.jetbrains.compose.ui.desktop)
    add(composeSkikoArtifacts.name, libs.jetbrains.compose.ui.geometry.desktop)
    add(composeSkikoArtifacts.name, libs.jetbrains.compose.ui.graphics.desktop)
    add(composeSkikoArtifacts.name, libs.jetbrains.compose.ui.text.desktop)
    add(composeSkikoArtifacts.name, libs.jetbrains.compose.ui.unit.desktop)
    add(composeSkikoArtifacts.name, libs.jetbrains.compose.ui.util.desktop)

    api(project(":skiko"))
    api(windowlessComposeJar)
    api(libs.jetbrains.compose.runtime)
    api(libs.jetbrains.compose.runtime.saveable)
    api("androidx.compose.runtime:runtime-retain:1.11.2")
    api("androidx.lifecycle:lifecycle-viewmodel:2.9.4")
    api("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.9.4")
    api("androidx.navigationevent:navigationevent:1.0.1")
    api(libs.androidx.annotation.jvm)
    api(libs.androidx.collection.jvm)

    runtimeOnly("org.jetbrains.kotlinx:atomicfu-jvm:0.28.0") // 有被CMP所依赖
}

val hiroCheckComposeSkikoPrototype by tasks.registering(CheckWindowlessComposeJarTask::class) {
    group = "hiro"
    description = "检查 Compose Skiko Android 原型所需类已进入窗口无关产物。"
    dependsOn(buildWindowlessComposeJar)
    inputJar.set(buildWindowlessComposeJar.flatMap { it.outputJar })

    requiredEntries.set(listOf("androidx/compose/ui/scene/CanvasLayersComposeScene_skikoKt.class", "androidx/compose/ui/scene/ComposeSceneRecomposer.class", "androidx/compose/ui/graphics/SkiaBackedCanvas.class", "androidx/compose/ui/graphics/SkiaBackedCanvas_skikoKt.class", "androidx/compose/ui/ComposeUiFlags_skikoKt.class", "androidx/compose/ui/SkikoComposeUiFlags.class", "androidx/compose/ui/text/intl/PlatformLocaleKt.class"))
    forbiddenFragments.set(listOf("AndroidComposeView", "android/graphics/RuntimeShader", "android/graphics/RenderEffect", "java/awt/", "javax/swing/", "javafx/", "androidx/compose/ui/awt/", "org/jetbrains/skiko/awt/", "androidx/compose/ui/platform/GlobalSnapshotManager_desktopKt.class", "androidx/compose/ui/Actuals_desktopKt.class", "androidx/compose/ui/platform/PlatformUriHandler_desktopKt.class", "androidx/compose/ui/platform/DesktopUriHandler", "androidx/compose/ui/text/intl/DesktopPlatformLocale", "androidx/compose/ui/platform/DisposableSaveableStateRegistry"))
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

tasks.named("check") { dependsOn(hiroCheckComposeSkikoPrototype) }
