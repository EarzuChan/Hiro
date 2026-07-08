import me.earzuchan.hiro.buildlogic.HiroBuildConfig
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    alias(libs.plugins.android.library)
    `maven-publish`
}

// TIPS：本机库打包的是我们地地道道的修复版

android {
    namespace = "${HiroBuildConfig.namespace}.skia"
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

tasks.withType<KotlinCompile>().configureEach { compilerOptions.moduleName.set("hiro-skia") }

dependencies { api("org.jetbrains.skiko:skiko-android:0.144.6") }

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "skia"
            val publication = this
            project.afterEvaluate { publication.from(project.components["release"]) }
        }
    }
}
