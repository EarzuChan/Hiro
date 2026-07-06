import me.earzuchan.hiro.buildlogic.HiroBuildConfig

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    alias(libs.plugins.android.library)
    `maven-publish`
}

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

dependencies {
    api("org.jetbrains.skiko:skiko-android:0.144.6")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "skia"
            val publication = this
            project.afterEvaluate { publication.from(project.components["release"]) }
        }
    }
}
