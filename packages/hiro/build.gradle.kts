import me.earzuchan.hiro.buildlogic.HiroBuildConfig

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "${HiroBuildConfig.namespace}.bundle"
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

dependencies {
    api(project(":skia"))
    api(project(":compose"))
}

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "hiro"
            val publication = this
            project.afterEvaluate { publication.from(project.components["release"]) }
        }
    }
}
