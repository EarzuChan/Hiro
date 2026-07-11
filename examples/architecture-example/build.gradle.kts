import me.earzuchan.hiro.buildlogic.HiroBuildConfig

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    id("me.earzuchan.hiro")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val appNamespace = "${HiroBuildConfig.namespace}.example.architecture"

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
    implementation(libs.androidx.activity)
    implementation("androidx.navigation3:navigation3-ui:1.1.1")
    implementation("io.insert-koin:koin-android:4.2.2") // 一夜一夜，这是特制的（允许在AndroidApp中开始Koin）
    implementation("io.insert-koin:koin-compose-viewmodel:4.2.2")
}
