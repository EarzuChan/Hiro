import com.android.build.gradle.tasks.MergeSourceSetFolders
import me.earzuchan.hiro.buildlogic.HiroBuildConfig

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    alias(libs.plugins.android.library)
    `maven-publish`
}

val skikoVersion = "0.144.6" // 对齐 CMP 所用的 Skiko 上游工件版本

val skiaNativeArm64 by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

val skiaNativeX64 by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

val generatedSkiaJniLibs = layout.buildDirectory.dir("generated/hiro/skiaAndroidJniLibs")

val unzipSkiaNativeArm64 = tasks.register<Copy>("unzipSkiaNativeArm64") {
    into(generatedSkiaJniLibs.map { it.dir("arm64-v8a") })
    from({ skiaNativeArm64.files.map { zipTree(it) } }) { include("libskiko-android-arm64.so") }
}

val unzipSkiaNativeX64 = tasks.register<Copy>("unzipSkiaNativeX64") {
    into(generatedSkiaJniLibs.map { it.dir("x86_64") })
    from({ skiaNativeX64.files.map { zipTree(it) } }) { include("libskiko-android-x64.so") }
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

    sourceSets { getByName("main") { jniLibs.directories.add(generatedSkiaJniLibs.get().asFile.absolutePath) } }
}

dependencies {
    api("org.jetbrains.skiko:skiko-android:$skikoVersion")

    skiaNativeArm64("org.jetbrains.skiko:skiko-android-runtime-arm64:$skikoVersion")
    skiaNativeX64("org.jetbrains.skiko:skiko-android-runtime-x64:$skikoVersion")
}

tasks.withType<MergeSourceSetFolders>().configureEach { if (name.endsWith("JniLibFolders")) dependsOn(unzipSkiaNativeArm64, unzipSkiaNativeX64) }

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "skia"
            val publication = this
            project.afterEvaluate { publication.from(project.components["release"]) }
        }
    }
}
