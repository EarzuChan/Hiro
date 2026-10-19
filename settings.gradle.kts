pluginManagement {
    includeBuild("build-logic")
    includeBuild("packages/hiro-gradle-plugin")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 仅用于在构建期取得 Skiko 并处理进 Hiro Skia，用户侧无需此仓库
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") { content { includeGroup("org.jetbrains.skiko") } }
    }
}

rootProject.name = "Hiro"

include(":skia")
project(":skia").projectDir = file("packages/skia")
include(":compose")
project(":compose").projectDir = file("packages/compose")
include(":material3")
project(":material3").projectDir = file("packages/material3")

include(":examples:compose-example")
include(":examples:material3-example")
include(":examples:third-party-example")
include(":examples:architecture-example")
include(":examples:view-lifecycle-example")
include(":examples:configuration-example")
include(":examples:ime-example")
