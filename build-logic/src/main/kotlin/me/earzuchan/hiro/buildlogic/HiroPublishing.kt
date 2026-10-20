package me.earzuchan.hiro.buildlogic

import org.gradle.api.publish.maven.MavenPublication

/** 为 Hiro 发布的工件填写统一的 POM 元数据 */
fun MavenPublication.configureHiroPom(displayName: String, desc: String) = pom {
    name.set(displayName)
    description.set(desc)
    url.set("https://github.com/EarzuChan/Hiro")

    licenses {
        license {
            name.set("MIT License")
            url.set("https://opensource.org/license/MIT")
        }
        license {
            name.set("Apache License 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0")
            comments.set("随附经处理的 Compose Multiplatform、AndroidX 与 Skiko，其余第三方作品见仓库中的 THIRD_PARTY_NOTICES.md")
        }
    }

    developers {
        developer {
            id.set("EarzuChan")
            name.set("EarzuChan")
            email.set("kontakt@earzuchan.me")
        }
    }

    scm {
        url.set("https://github.com/EarzuChan/Hiro")
        connection.set("scm:git:https://github.com/EarzuChan/Hiro.git")
    }
}
