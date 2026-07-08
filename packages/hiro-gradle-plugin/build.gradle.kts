import me.earzuchan.hiro.buildlogic.HiroBuildConfig
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    `maven-publish`
}

val gradlePluginNamespace = "${HiroBuildConfig.namespace}.gradleplugin"
group = gradlePluginNamespace
version = HiroBuildConfig.version

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }

dependencies { implementation(gradleApi()) }

gradlePlugin {
    plugins {
        create("hiro") {
            id = HiroBuildConfig.namespace // 这里为了用户引入，仍用主NS
            implementationClass = "$gradlePluginNamespace.HiroGradlePlugin"
            displayName = "Hiro Compose Skia Android"
            description = "配置 Hiro Compose Skia Android 的依赖剥离和兼容性检查"
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = when (name) {
            "pluginMaven" -> "hiro-gradle-plugin"

            else -> artifactId
        }
    }
}
