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
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
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
