import me.earzuchan.hiro.buildlogic.HiroBuildConfig

plugins {
    id("me.earzuchan.hiro.internal.build-logic")
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

allprojects {
    group = HiroBuildConfig.namespace
    version = HiroBuildConfig.version
}

tasks.register("hiroBuildAll") {
    group = "hiro"
    description = "构建 Hiro 的所有模块和示例"
    dependsOn(subprojects.filter { it.buildFile.isFile }.map { "${it.path}:build" })
    dependsOn(gradle.includedBuild("hiro-gradle-plugin").task(":build"))
}

tasks.register("hiroPublishLocal") {
    group = "hiro"
    description = "发布 Hiro 的公开坐标到 Maven Local"
    dependsOn(":skia:publishToMavenLocal", ":compose:publishToMavenLocal", ":material3:publishToMavenLocal")
    dependsOn(gradle.includedBuild("hiro-gradle-plugin").task(":publishToMavenLocal"))
}
