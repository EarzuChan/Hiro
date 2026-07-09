package me.earzuchan.hiro.gradleplugin

import me.earzuchan.hiro.gradleplugin.misc.HiroAndroidPackaging
import me.earzuchan.hiro.gradleplugin.misc.HiroAttributes
import me.earzuchan.hiro.gradleplugin.processing.HiroDependenciesManageRule
import me.earzuchan.hiro.gradleplugin.processing.HiroVariantKindDisambiguationRule
import me.earzuchan.hiro.gradleplugin.verdict.HiroFinalVerdict
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

// TIPS：HGP剔除普通包中对Compose的依赖，使Kmp包能采用Hiro特制Skiko系变体，并校验最终类路径+原安卓/桌面特有类掺入情况

enum class HiroBinaryLeakAction { Fail, Warn }

open class HiroExtension {
    var binaryLeakAction: HiroBinaryLeakAction = HiroBinaryLeakAction.Fail // 默认值
}

class HiroGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("hiro", HiroExtension::class.java) // 读取用户设置
        val finalVerdict = HiroFinalVerdict(project, extension) // 实例化最终审判

        // 对不该进 APK 的 KMP 元数据/源码集结构信息进行排除，以防止 APK packaging 噪音、重复资源、无意义元数据混入
        project.pluginManager.withPlugin("com.android.application", Action { HiroAndroidPackaging.addMetadataExcludes(project) })
        project.pluginManager.withPlugin("com.android.library", Action { HiroAndroidPackaging.addMetadataExcludes(project) })

        // 配置规矩以在多个Hiro变体中选最优（Hiro变体们是已在HiroDependenciesManageRule的处理中加入的）
        project.dependencies.attributesSchema.attribute(HiroAttributes.hiroVariantKind, Action { it.disambiguationRules.add(HiroVariantKindDisambiguationRule::class.java) })

        // 进行对本工程的每个依赖项进行深度（这个是Gradle保证的）包剥离+变体选入
        project.dependencies.components.all(HiroDependenciesManageRule::class.java)

        // 对每个配置（即依赖的分组。如comp、rt）进行拨弄
        project.configurations.configureEach(Action { configuration ->
            if (!configuration.isAndroidMainClasspath()) return@Action

            project.logger.lifecycle("Hiro Gradle 插件：接管了 ${project.path} 的 ${configuration.name} 分组")
            configuration.attributes.attribute(HiroAttributes.hiroVariant, true) // 对暗号，以使得我们在DepsManRule创建的特色变体生效

            finalVerdict.perform(configuration)
        })
    }

    private fun Configuration.isAndroidMainClasspath(): Boolean {
        if (!isCanBeResolved) return false

        val loweredName = name.lowercase()
        val isMainClasspath = loweredName.endsWith("compileclasspath") || loweredName.endsWith("runtimeclasspath")
        val isTestClasspath = loweredName.contains("test")

        return isMainClasspath && !isTestClasspath
    }
}
