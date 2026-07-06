import com.android.build.gradle.tasks.MergeSourceSetFolders
import me.earzuchan.hiro.buildlogic.HiroBuildConfig

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    alias(libs.plugins.android.library)
    `maven-publish`
}

val skikoVersion = "0.144.6" // 对齐 CMP 所用的 Skiko 版本

val skikoNativeArm64 by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

val skikoNativeX64 by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

val generatedSkikoJniLibs = layout.buildDirectory.dir("generated/hiro/skikoAndroidJniLibs")

val unzipSkikoNativeArm64 = tasks.register<Copy>("unzipSkikoNativeArm64") {
    into(generatedSkikoJniLibs.map { it.dir("arm64-v8a") })
    from({ skikoNativeArm64.files.map { zipTree(it) } }) { include("libskiko-android-arm64.so") }
}

val unzipSkikoNativeX64 = tasks.register<Copy>("unzipSkikoNativeX64") {
    into(generatedSkikoJniLibs.map { it.dir("x86_64") })
    from({ skikoNativeX64.files.map { zipTree(it) } }) { include("libskiko-android-x64.so") }
}

android {
    namespace = "${HiroBuildConfig.namespace}.skiko"
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

    sourceSets { getByName("main") { jniLibs.directories.add(generatedSkikoJniLibs.get().asFile.absolutePath) } }
}

dependencies {
    api("org.jetbrains.skiko:skiko-android:$skikoVersion")

    skikoNativeArm64("org.jetbrains.skiko:skiko-android-runtime-arm64:$skikoVersion")
    skikoNativeX64("org.jetbrains.skiko:skiko-android-runtime-x64:$skikoVersion")
}

tasks.withType<MergeSourceSetFolders>().configureEach { if (name.endsWith("JniLibFolders")) dependsOn(unzipSkikoNativeArm64, unzipSkikoNativeX64) }

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "skiko"
            val publication = this
            project.afterEvaluate { publication.from(project.components["release"]) }
        }
    }
}
