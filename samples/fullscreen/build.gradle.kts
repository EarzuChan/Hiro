import me.earzuchan.hiro.buildlogic.HiroBuildConfig

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    alias(libs.plugins.android.application)
}

val appNamespace = "${HiroBuildConfig.namespace}.samples.fullscreen"

android {
    namespace = appNamespace
    compileSdk = HiroBuildConfig.androidCompileSdk

    defaultConfig {
        applicationId = appNamespace
        minSdk = HiroBuildConfig.androidMinSdk
        targetSdk = HiroBuildConfig.androidCompileSdk
        versionCode = 1
        versionName = HiroBuildConfig.version
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies { implementation(project(":hiro")) }
