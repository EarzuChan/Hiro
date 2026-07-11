import me.earzuchan.hiro.buildlogic.HiroBuildConfig

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    id("me.earzuchan.hiro")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val appNamespace = "${HiroBuildConfig.namespace}.example.material3"

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
    implementation(project(":compose"))
    implementation(project(":material3"))
    implementation(libs.androidx.activity)
}
