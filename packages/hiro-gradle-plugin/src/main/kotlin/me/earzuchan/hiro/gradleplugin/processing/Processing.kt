package me.earzuchan.hiro.gradleplugin.processing

import me.earzuchan.hiro.gradleplugin.misc.HiroAttributes
import me.earzuchan.hiro.gradleplugin.misc.HiroDependencyPolicy
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataDetails
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.DependenciesMetadata
import org.gradle.api.artifacts.DependencyMetadata
import org.gradle.api.artifacts.VariantMetadata
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.MultipleCandidatesDetails
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.logging.Logging
import java.util.Collections

internal enum class HiroVariantKind(val wireName: String) {
    Skiko("skiko"),
    Desktop("desktop"),
    Jvm("jvm");

    companion object {
        val priority: List<HiroVariantKind> = listOf(Skiko, Desktop, Jvm)

        fun fromVariantName(variantName: String) = when {
            variantName.startsWith("skiko", ignoreCase = true) -> Skiko

            variantName.startsWith("desktop", ignoreCase = true) -> Desktop

            else -> Jvm
        }
    }
}


internal abstract class HiroVariantKindDisambiguationRule : AttributeDisambiguationRule<String> {
    override fun execute(details: MultipleCandidatesDetails<String>) {
        // 按我的顺序选择变体
        HiroVariantKind.priority.firstOrNull { kind -> details.candidateValues.contains(kind.wireName) }?.let { kind -> details.closestMatch(kind.wireName) }
    }
}

private object HiroDependenciesStripper {
    private val logger = Logging.getLogger(HiroDependenciesStripper::class.java)
    private val logged = Collections.synchronizedSet(linkedSetOf<String>())

    fun <T : DependencyMetadata<T>> strip(metadata: DependenciesMetadata<T>, owner: String, kind: String) {
        val iterator = metadata.iterator()

        while (iterator.hasNext()) {
            val dependency = iterator.next()

            val isComposeModuleOrJbrApi = HiroDependencyPolicy.isComposeModuleOrJbrApi(dependency.group, dependency.name)
            if (!isComposeModuleOrJbrApi) continue // 如果不是官方 Compose 或 JBR API 的引入就不管

            val notation = "${dependency.group}:${dependency.name}:${dependency.versionConstraint.displayName}"
            val logKey = "$owner|$kind|$notation"

            if (logged.add(logKey)) logger.lifecycle("Hiro Gradle 插件：从 $owner 移除：$kind $notation")

            iterator.remove()
        }
    }
}

abstract class HiroDependenciesManageRule : ComponentMetadataRule {
    override fun execute(context: ComponentMetadataContext) { // 对于经停本管线的每一个依赖
        val details = context.details
        val id = details.id
        val group = id.group
        val name = id.name
        val owner = "$group:$name:${id.version}"

        if (HiroDependencyPolicy.isHiroModule(group)) return // Hiro 包，直接放行。没有 Hiro 包，用户会自己无法使用 Compose Api。所以不需要我炸

        // 不让这些进入
        if (HiroDependencyPolicy.isComposeModuleOrJbrApi(group, name)) throw GradleException("Hiro Gradle 插件：发现有官方 Compose 模块 / JetbrainsRuntime API 进入依赖解析，请您确保您没有直接引入 $owner")

        details.allVariants(stripDependencies(owner)) // 对包的依赖剥离

        if (!HiroDependencyPolicy.isThirdPartyKmpCandidate(group)) return // 如果不是第三方 KMP 的候选：放行

        // 以原KMP库的这些变体做出Hiro特有变体（欺骗为安卓、并打专门消费的标，使得Gradle可为安卓取用。并进行深度剥离）
        details.addHiroVariant("hiroSkikoApiElements", "skikoApiElements", Usage.JAVA_API, owner)
        details.addHiroVariant("hiroSkikoRuntimeElements", "skikoRuntimeElements", Usage.JAVA_RUNTIME, owner)
        details.addHiroVariant("hiroPublishedSkikoApiElements", "skikoApiElements-published", Usage.JAVA_API, owner)
        details.addHiroVariant("hiroPublishedSkikoRuntimeElements", "skikoRuntimeElements-published", Usage.JAVA_RUNTIME, owner)

        details.addHiroVariant("hiroDesktopApiElements", "desktopApiElements", Usage.JAVA_API, owner)
        details.addHiroVariant("hiroDesktopRuntimeElements", "desktopRuntimeElements", Usage.JAVA_RUNTIME, owner)
        details.addHiroVariant("hiroPublishedDesktopApiElements", "desktopApiElements-published", Usage.JAVA_API, owner)
        details.addHiroVariant("hiroPublishedDesktopRuntimeElements", "desktopRuntimeElements-published", Usage.JAVA_RUNTIME, owner)

        details.addHiroVariant("hiroJvmApiElements", "jvmApiElements", Usage.JAVA_API, owner)
        details.addHiroVariant("hiroJvmRuntimeElements", "jvmRuntimeElements", Usage.JAVA_RUNTIME, owner)
        details.addHiroVariant("hiroPublishedJvmApiElements", "jvmApiElements-published", Usage.JAVA_API, owner)
        details.addHiroVariant("hiroPublishedJvmRuntimeElements", "jvmRuntimeElements-published", Usage.JAVA_RUNTIME, owner)

        details.blockAndroidVariantsFromHiroClasspath()
    }

    private fun ComponentMetadataDetails.addHiroVariant(variantName: String, baseVariantName: String, usage: String, owner: String) = maybeAddVariant(
        variantName, baseVariantName, makeHiroVariant(
            usage = usage,
            variantKind = HiroVariantKind.fromVariantName(baseVariantName),
            owner = owner,
        )
    )

    // 制作特色变体
    private fun makeHiroVariant(usage: String, variantKind: HiroVariantKind, owner: String): Action<VariantMetadata> = Action { variant ->
        variant.attributes(Action { attributes ->
            attributes.attribute(HiroAttributes.hiroVariant, true) // 写入暗号，届时选用这个
            attributes.attribute(HiroAttributes.hiroVariantKind, variantKind.wireName)

            attributes.attribute(Category.CATEGORY_ATTRIBUTE, attributes.named(Category::class.java, Category.LIBRARY))
            attributes.attribute(Usage.USAGE_ATTRIBUTE, attributes.named(Usage::class.java, usage))
            attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, attributes.named(LibraryElements::class.java, LibraryElements.JAR))
            attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, attributes.named(Bundling::class.java, Bundling.EXTERNAL))

            attributes.attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, attributes.named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.ANDROID))
            attributes.attribute(kotlinPlatformTypeAttribute, "androidJvm")
        })

        stripDependencies(owner).execute(variant) // 顺带在变体内深度剥离
    }

    private fun ComponentMetadataDetails.blockAndroidVariantsFromHiroClasspath() = androidVariantNames.forEach { variantName ->
        withVariant(variantName, Action { variant ->
            variant.attributes(Action { attributes -> attributes.attribute(HiroAttributes.hiroVariant, false) })
        })
    }

    private companion object {
        private val logger = Logging.getLogger(HiroDependenciesManageRule::class.java)

        val kotlinPlatformTypeAttribute: Attribute<String> = Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java)

        val androidVariantNames = listOf(
            "androidApiElements",
            "androidRuntimeElements",
            "androidApiElements-published",
            "androidRuntimeElements-published",
        )

        fun stripDependencies(owner: String): Action<VariantMetadata> = Action { variant ->
            variant.withDependencies(Action { dependencies -> HiroDependenciesStripper.strip(dependencies, owner, "dependency") })

            variant.withDependencyConstraints(Action { constraints -> HiroDependenciesStripper.strip(constraints, owner, "constraint") })
        }
    }
}

