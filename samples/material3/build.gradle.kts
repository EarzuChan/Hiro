import me.earzuchan.hiro.buildlogic.HiroBuildConfig

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val appNamespace = "${HiroBuildConfig.namespace}.samples.material3"

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

dependencies {
    implementation(project(":hiro"))
    implementation(project(":material3"))
}
