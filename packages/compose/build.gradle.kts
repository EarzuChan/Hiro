import me.earzuchan.hiro.buildlogic.HiroBuildConfig

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    id("me.earzuchan.hiro")
    alias(libs.plugins.android.library)
    `maven-publish`
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

dependencies {
    api(project(":skiko"))
    api(libs.jetbrains.compose.runtime)
    api(libs.jetbrains.compose.runtime.saveable)
    api(libs.jetbrains.compose.ui)
    api(libs.jetbrains.compose.ui.backhandler)
    api(libs.jetbrains.compose.ui.geometry)
    api(libs.jetbrains.compose.ui.graphics)
    api(libs.jetbrains.compose.ui.text)
    api(libs.jetbrains.compose.ui.unit)
    api(libs.jetbrains.compose.ui.util)
    api(libs.jetbrains.compose.foundation)
    api(libs.jetbrains.compose.foundation.layout)
    api(libs.jetbrains.compose.animation)
    api(libs.jetbrains.compose.animation.core)
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
