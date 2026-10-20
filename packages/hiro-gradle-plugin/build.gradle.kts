import me.earzuchan.hiro.buildlogic.HiroBuildConfig
import me.earzuchan.hiro.buildlogic.configureHiroPom
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

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

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)

        // 插件运行在 Gradle 内置的 Kotlin 标准库上，语言与 API 版本须与之对齐
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        apiVersion.set(KotlinVersion.KOTLIN_2_2)
    }
}

dependencies {
    implementation(gradleApi())

    // 平台链接转换改写第三方字节码所用。与 AGP 共处同一类加载器，运行时可能用到 AGP 带来的较旧 ASM，故只用 ASM 9 早期即有的 API
    implementation("org.ow2.asm:asm:9.8")
    implementation("org.ow2.asm:asm-commons:9.8")

    // 仅编译期依赖，运行时使用用户构建中已有的 AGP 与 Compose 插件；Compose 插件版本须与 Hiro 所跟随的 CMP 一致
    compileOnly("com.android.tools.build:gradle-api:${libs.versions.agp.get()}")
    compileOnly("org.jetbrains.compose:compose-gradle-plugin:1.12.2")
}

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
        configureHiroPom("Hiro Gradle Plugin", "为使用 Hiro 的安卓工程剥离官方 Compose 依赖、选用 Skiko 变体，并代为生成 Compose 资源")
        artifactId = when (name) {
            "pluginMaven" -> "hiro-gradle-plugin"

            else -> artifactId
        }
    }
}
