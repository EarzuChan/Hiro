import me.earzuchan.hiro.buildlogic.HiroBuildConfig
import me.earzuchan.hiro.buildlogic.processToHiroJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    alias(libs.plugins.android.library)
    `maven-publish`
}

val material3Jar = processToHiroJar("material3") {
    artifact("org.jetbrains.compose.material3:material3-desktop:1.11.0-alpha07")

    dropPathFragment("Awt", "Swing", "JPopup", "ComposeContainer", "ComposeSceneMediator", "DesktopComposeSceneLayer", "WindowComposeSceneLayer")

    dropBinaryPattern("androidx/compose/ui/platform/AndroidComposeView", "androidx/compose/ui/platform/AndroidOwner", "androidx/compose/ui/platform/RenderNodeLayer", "androidx/compose/ui/platform/ViewLayer", "android/graphics/RuntimeShader", "android/graphics/RenderEffect", "java/awt/", "javax/swing/", "javafx/", "androidx/compose/ui/awt/", "org/jetbrains/skiko/awt/")

    requireJarEntry("androidx/compose/material3/ButtonKt.class", "androidx/compose/material3/MaterialThemeKt.class")

    forbidJarEntryFragment("AndroidComposeView", "android/graphics/RuntimeShader", "android/graphics/RenderEffect", "java/awt/", "javax/swing/", "javafx/", "androidx/compose/ui/awt/", "org/jetbrains/skiko/awt/")
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

tasks.withType<KotlinCompile>().configureEach { compilerOptions.moduleName.set("hiro-material3") }

dependencies {
    api(project(":compose"))
    api(material3Jar.files)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "material3"
            val publication = this
            project.afterEvaluate { publication.from(project.components["release"]) }
        }
    }
}

tasks.named("check") { dependsOn(material3Jar) }
