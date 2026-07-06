package me.earzuchan.hiro.gradleplugin

import org.gradle.api.Action
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataDetails
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.DirectDependenciesMetadata
import org.gradle.api.artifacts.VariantMetadata
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment

abstract class HiroSkiaVariantRule : ComponentMetadataRule {
    override fun execute(context: ComponentMetadataContext) {
        val details = context.details
        val id = details.id

        if (HiroDependencyPolicy.isComposeLibrary(id.group)) {
            details.blockComposeModuleFromHiroClasspath()
            return
        }

        if (!HiroDependencyPolicy.isThirdPartyHijackCandidate(id.group)) return

        details.allVariants(Action { variant -> variant.withDependencies(blockComposeDependenciesAction) })

        details.addHiroKmpCandidateVariant("hiroSkiaSkikoApiElements", "skikoApiElements", Usage.JAVA_API)
        details.addHiroKmpCandidateVariant("hiroSkiaSkikoRuntimeElements", "skikoRuntimeElements", Usage.JAVA_RUNTIME)
        details.addHiroKmpCandidateVariant("hiroSkiaPublishedSkikoApiElements", "skikoApiElements-published", Usage.JAVA_API,)
        details.addHiroKmpCandidateVariant("hiroSkiaPublishedSkikoRuntimeElements", "skikoRuntimeElements-published", Usage.JAVA_RUNTIME,)
        details.addHiroKmpCandidateVariant("hiroSkiaJvmApiElements", "jvmApiElements", Usage.JAVA_API)
        details.addHiroKmpCandidateVariant("hiroSkiaJvmRuntimeElements", "jvmRuntimeElements", Usage.JAVA_RUNTIME)
        details.addHiroKmpCandidateVariant("hiroSkiaPublishedJvmApiElements", "jvmApiElements-published", Usage.JAVA_API,)
        details.addHiroKmpCandidateVariant("hiroSkiaPublishedJvmRuntimeElements", "jvmRuntimeElements-published", Usage.JAVA_RUNTIME,)
        details.addHiroKmpCandidateVariant("hiroSkiaDesktopApiElements", "desktopApiElements", Usage.JAVA_API)
        details.addHiroKmpCandidateVariant("hiroSkiaDesktopRuntimeElements", "desktopRuntimeElements", Usage.JAVA_RUNTIME)
        details.addHiroKmpCandidateVariant("hiroSkiaPublishedDesktopApiElements", "desktopApiElements-published", Usage.JAVA_API)
        details.addHiroKmpCandidateVariant("hiroSkiaPublishedDesktopRuntimeElements", "desktopRuntimeElements-published", Usage.JAVA_RUNTIME,)
    }

    private fun ComponentMetadataDetails.addHiroKmpCandidateVariant(
        variantName: String,
        baseVariantName: String,
        usage: String,
    ) = maybeAddVariant(
        variantName,
        baseVariantName,
        hiroVariantAction(
            usage,
            TargetJvmEnvironment.ANDROID,
            HiroKmpVariantKind.fromVariantName(baseVariantName),
        ),
    )

    private fun hiroVariantAction(usage: String, jvmEnvironment: String, variantKind: HiroKmpVariantKind): Action<VariantMetadata> = Action { variant ->
        variant.attributes(Action { attributes ->
            attributes.attribute(HiroAttributes.skiaBackend, HiroAttributes.required)
            attributes.attribute(HiroAttributes.kmpVariantKind, variantKind.wireName)
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, attributes.named(Category::class.java, Category.LIBRARY),)
            attributes.attribute(Usage.USAGE_ATTRIBUTE, attributes.named(Usage::class.java, usage),)
            attributes.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, attributes.named(LibraryElements::class.java, LibraryElements.JAR),)
            attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, attributes.named(Bundling::class.java, Bundling.EXTERNAL),)
            attributes.attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, attributes.named(TargetJvmEnvironment::class.java, jvmEnvironment),)
            attributes.attribute(kotlinPlatformTypeAttribute, "jvm")
        })

        variant.withDependencies(blockComposeDependenciesAction)
    }

    private fun ComponentMetadataDetails.blockComposeModuleFromHiroClasspath() = allVariants(Action { variant -> variant.attributes(Action { attributes -> attributes.attribute(HiroAttributes.skiaBackend, HiroAttributes.blockedCompose) }) })

    private companion object {
        val kotlinPlatformTypeAttribute = Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java)

        val blockComposeDependenciesAction = Action<DirectDependenciesMetadata> { dependencies -> HiroComposeDependencyBlocker.removeComposeDependencies(dependencies) }
    }
}
