import me.earzuchan.hiro.buildlogic.HiroBuildConfig
import me.earzuchan.hiro.buildlogic.configureHiroPom
import me.earzuchan.hiro.buildlogic.processToHiroJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    alias(libs.plugins.android.library)
    `maven-publish`
}

val material3Jar = processToHiroJar("material3") {
    artifact("org.jetbrains.compose.material3:material3-desktop:1.12.0-alpha03")

    bundleLegalFiles("material3")

    // 平台水波纹入口由 Hiro 顶替
    dropPathPrefix("androidx/compose/material3/internal/ripple/Ripple_skikoKt")

    dropPathFragment("Awt", "Swing", "JPopup", "ComposeContainer", "ComposeSceneMediator", "DesktopComposeSceneLayer", "WindowComposeSceneLayer")

    dropBinaryPattern("androidx/compose/ui/platform/AndroidComposeView", "androidx/compose/ui/platform/AndroidOwner", "androidx/compose/ui/platform/RenderNodeLayer", "androidx/compose/ui/platform/ViewLayer", "android/graphics/RuntimeShader", "android/graphics/RenderEffect", "java/awt/", "javax/swing/", "javafx/", "androidx/compose/ui/awt/", "org/jetbrains/skiko/awt/")

    requireJarEntry("androidx/compose/material3/ButtonKt.class", "androidx/compose/material3/MaterialThemeKt.class", "androidx/compose/material3/internal/ripple/RippleNode.class", "androidx/compose/material3/internal/ripple/RippleNodeConfig.class")

    forbidJarEntryFragment("AndroidComposeView", "android/graphics/RuntimeShader", "android/graphics/RenderEffect", "java/awt/", "javax/swing/", "javafx/", "androidx/compose/ui/awt/", "org/jetbrains/skiko/awt/", "androidx/compose/material3/internal/ripple/Ripple_skikoKt")
}

android {
    namespace = "${HiroBuildConfig.namespace}.material3"
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

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.moduleName.set("hiro-material3")
    compilerOptions.optIn.add("me.earzuchan.hiro.log.HiroInternalApi")
}

dependencies {
    api(project(":compose"))
    api(material3Jar.files)

    // Material3 上游所依赖、不属于 Hiro 接管范围的模块：形状变形（MaterialShapes、LoadingIndicator）与日期模型（DatePicker）
    api("androidx.graphics:graphics-shapes:1.1.0")
    api("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "material3"
            configureHiroPom("Hiro Material 3", "Hiro Compose 的 Material 3，含复刻 AOSP 的水波纹")
            val publication = this
            project.afterEvaluate { publication.from(project.components["release"]) }
        }
    }
}

tasks.named("check") { dependsOn(material3Jar) }
