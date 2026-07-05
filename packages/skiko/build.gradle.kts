import me.earzuchan.hiro.buildlogic.HiroBuildConfig
import me.earzuchan.hiro.buildlogic.task.CheckSkikoAndroidJniLibsTask
import me.earzuchan.hiro.buildlogic.task.UnpackSkikoAndroidRuntimeTask

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    alias(libs.plugins.android.library)
    `maven-publish`
}

val skikoAndroidRuntimeArm64 by configurations.registering {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

val skikoAndroidRuntimeX64 by configurations.registering {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

val unpackSkikoAndroidRuntime by tasks.registering(UnpackSkikoAndroidRuntimeTask::class) {
    arm64Runtime.from(skikoAndroidRuntimeArm64)
    x64Runtime.from(skikoAndroidRuntimeX64)
    outputDirectory.set(layout.buildDirectory.dir("generated/hiro/skikoAndroidJniLibs"))
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
}

androidComponents {
    onVariants { variant ->
        val jniLibs = checkNotNull(variant.sources.jniLibs) {
            "当前 Android 变体没有 jniLibs 源目录入口：${variant.name}"
        }
        jniLibs.addGeneratedSourceDirectory(
            unpackSkikoAndroidRuntime,
            UnpackSkikoAndroidRuntimeTask::outputDirectory,
        )
    }
}

dependencies {
    api(libs.jetbrains.skiko)
    add(skikoAndroidRuntimeArm64.name, libs.jetbrains.skiko.android.runtime.arm64)
    add(skikoAndroidRuntimeX64.name, libs.jetbrains.skiko.android.runtime.x64)
}

tasks.register<CheckSkikoAndroidJniLibsTask>("hiroCheckSkikoAndroidJniLibs") {
    group = "hiro"
    description = "检查 Skiko Android native runtime 已按 Android ABI 目录展开。"
    dependsOn(unpackSkikoAndroidRuntime)
    inputDirectory.set(unpackSkikoAndroidRuntime.flatMap { it.outputDirectory })
}

tasks.named("check") {
    dependsOn("hiroCheckSkikoAndroidJniLibs")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "skiko"
            val publication = this
            project.afterEvaluate { publication.from(project.components["release"]) }
        }
    }
}
