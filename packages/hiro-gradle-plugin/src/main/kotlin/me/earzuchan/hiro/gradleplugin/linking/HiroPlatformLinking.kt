package me.earzuchan.hiro.gradleplugin.linking

import me.earzuchan.hiro.gradleplugin.misc.HiroAttributes
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * 平台重新链接的接线：HGP 把第三方 KMP 包换成桌面变体，就由 HGP 负责把其中的桌面平台类型链接回安卓
 *
 * 按 Gradle 的产物转换惯例：Jar 产物默认标为“未链接”，Hiro 接管的类路径请求“已链接”，二者之间由 [HiroPlatformLinkTransform] 衔接。
 * 该属性只落在产物层，不出现在变体元数据中，因此不影响依赖图的变体选择
 */
internal object HiroPlatformLinking {
    fun configure(project: Project) {
        val dependencies = project.dependencies

        dependencies.attributesSchema.attribute(HiroAttributes.hiroPlatformLinked)

        // jar 产物类型由 JVM 生态插件创建，抢先创建会与之冲突；故随其出现时打标，评估结束仍无则补建
        dependencies.artifactTypes.matching { it.name == JAR }.configureEach(Action { it.attributes.attribute(HiroAttributes.hiroPlatformLinked, false) })
        project.afterEvaluate(Action { dependencies.artifactTypes.maybeCreate(JAR) })

        dependencies.registerTransform(HiroPlatformLinkTransform::class.java, Action { spec ->
            spec.from.attribute(HiroAttributes.artifactType, JAR).attribute(HiroAttributes.hiroPlatformLinked, false)
            spec.to.attribute(HiroAttributes.artifactType, JAR).attribute(HiroAttributes.hiroPlatformLinked, true)
        })
    }

    fun requestLinked(configuration: Configuration) {
        configuration.attributes.attribute(HiroAttributes.hiroPlatformLinked, true)
    }

    private const val JAR = "jar"
}
