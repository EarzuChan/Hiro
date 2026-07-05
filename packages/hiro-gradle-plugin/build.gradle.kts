import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }

sourceSets {
    main {
        kotlin.srcDir("../../build-logic/src/hiroGradlePlugin/kotlin")
    }
}

dependencies { implementation(gradleApi()) }

gradlePlugin {
    plugins {
        create("hiro") {
            id = "me.earzuchan.hiro"
            implementationClass = "me.earzuchan.hiro.gradle.HiroGradlePlugin"
            displayName = "Hiro Compose Skiko Android"
            description = "配置 Hiro Compose Skiko Android 的依赖替换和兼容性检查"
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
