package me.earzuchan.hiro.gradle

import org.gradle.api.Action
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataDetails
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.DirectDependenciesMetadata
import org.gradle.api.artifacts.MutableVariantFilesMetadata
import org.gradle.api.artifacts.VariantMetadata
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment

abstract class HiroSkikoVariantRule : ComponentMetadataRule {
    override fun execute(context: ComponentMetadataContext) {
        val details = context.details
        val id = details.id

        if (HiroDependencyPolicy.isComposeMultiplatformModule(id.group)) {
            details.addHiroSkikoVariantFromMetadata("hiroSkikoApiElements", Usage.JAVA_API)
            details.addHiroSkikoVariantFromMetadata("hiroSkikoRuntimeElements", Usage.JAVA_RUNTIME)
            return
        }

        if (!HiroDependencyPolicy.isThirdPartyHijackCandidate(id.group)) return

        details.allVariants(object : Action<VariantMetadata> {
            override fun execute(variant: VariantMetadata) {
                variant.withDependencies(object : Action<DirectDependenciesMetadata> {
                    override fun execute(dependencies: DirectDependenciesMetadata) {
                        HiroDependencyRewriter.rewriteMetadataDependencies(id.group, dependencies)
                    }
                })
            }
        })

        details.addHiroSkikoCandidateVariant("hiroSkikoSkikoApiElements", "skikoApiElements", Usage.JAVA_API)
        details.addHiroSkikoCandidateVariant("hiroSkikoSkikoRuntimeElements", "skikoRuntimeElements", Usage.JAVA_RUNTIME)
        details.addHiroSkikoCandidateVariant(
            "hiroSkikoPublishedSkikoApiElements",
            "skikoApiElements-published",
            Usage.JAVA_API,
        )
        details.addHiroSkikoCandidateVariant(
            "hiroSkikoPublishedSkikoRuntimeElements",
            "skikoRuntimeElements-published",
            Usage.JAVA_RUNTIME,
        )
        details.addHiroSkikoCandidateVariant("hiroSkikoJvmApiElements", "jvmApiElements", Usage.JAVA_API)
        details.addHiroSkikoCandidateVariant("hiroSkikoJvmRuntimeElements", "jvmRuntimeElements", Usage.JAVA_RUNTIME)
        details.addHiroSkikoCandidateVariant(
            "hiroSkikoPublishedJvmApiElements",
            "jvmApiElements-published",
            Usage.JAVA_API,
        )
        details.addHiroSkikoCandidateVariant(
            "hiroSkikoPublishedJvmRuntimeElements",
            "jvmRuntimeElements-published",
            Usage.JAVA_RUNTIME,
        )
        details.addHiroSkikoCandidateVariant("hiroSkikoDesktopApiElements", "desktopApiElements", Usage.JAVA_API)
        details.addHiroSkikoCandidateVariant("hiroSkikoDesktopRuntimeElements", "desktopRuntimeElements", Usage.JAVA_RUNTIME)
        details.addHiroSkikoCandidateVariant("hiroSkikoPublishedDesktopApiElements", "desktopApiElements-published", Usage.JAVA_API)
        details.addHiroSkikoCandidateVariant(
            "hiroSkikoPublishedDesktopRuntimeElements",
            "desktopRuntimeElements-published",
            Usage.JAVA_RUNTIME,
        )
    }

    private fun ComponentMetadataDetails.addHiroSkikoVariantFromMetadata(
        variantName: String,
        usage: String,
    ) {
        maybeAddVariant(
            variantName,
            "metadataApiElements",
            hiroVariantAction(
                usage = usage,
                jvmEnvironment = TargetJvmEnvironment.ANDROID,
                candidateKind = HiroSkikoCandidateKind.composeMetadata,
                trimComposeMetadataDependencies = true,
                removeRuntimeFiles = usage == Usage.JAVA_RUNTIME,
            ),
        )
    }

    private fun ComponentMetadataDetails.addHiroSkikoCandidateVariant(
        variantName: String,
        baseVariantName: String,
        usage: String,
    ) {
        maybeAddVariant(
            variantName,
            baseVariantName,
            hiroVariantAction(
                usage = usage,
                jvmEnvironment = TargetJvmEnvironment.ANDROID,
                candidateKind = candidateKindFor(baseVariantName),
            ),
        )
    }

    private fun candidateKindFor(baseVariantName: String): String {
        val normalized = baseVariantName.removeSuffix("-published")
        return when {
            normalized.startsWith("skiko", ignoreCase = true) -> HiroSkikoCandidateKind.skiko
            normalized.startsWith("jvm", ignoreCase = true) -> HiroSkikoCandidateKind.jvm
            normalized.startsWith("desktop", ignoreCase = true) -> HiroSkikoCandidateKind.desktop
            else -> error("不是 Hiro 可识别的 Skiko/JVM 候选变体：$baseVariantName")
        }
    }

    private fun hiroVariantAction(
        usage: String,
        jvmEnvironment: String,
        candidateKind: String,
        trimComposeMetadataDependencies: Boolean = false,
        removeRuntimeFiles: Boolean = false,
    ): Action<VariantMetadata> =
        object : Action<VariantMetadata> {
            override fun execute(variant: VariantMetadata) {
                variant.attributes(object : Action<AttributeContainer> {
                    override fun execute(attributes: AttributeContainer) {
                        attributes.attribute(HiroAttributes.skikoBackend, HiroAttributes.required)
                        attributes.attribute(HiroAttributes.skikoCandidateKind, candidateKind)
                        attributes.attribute(
                            Category.CATEGORY_ATTRIBUTE,
                            attributes.named(Category::class.java, Category.LIBRARY),
                        )
                        attributes.attribute(
                            Usage.USAGE_ATTRIBUTE,
                            attributes.named(Usage::class.java, usage),
                        )
                        attributes.attribute(
                            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                            attributes.named(LibraryElements::class.java, LibraryElements.JAR),
                        )
                        attributes.attribute(
                            Bundling.BUNDLING_ATTRIBUTE,
                            attributes.named(Bundling::class.java, Bundling.EXTERNAL),
                        )
                        attributes.attribute(
                            TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                            attributes.named(TargetJvmEnvironment::class.java, jvmEnvironment),
                        )
                        attributes.attribute(kotlinPlatformTypeAttribute, "jvm")
                    }
                })
                if (trimComposeMetadataDependencies) {
                    variant.withDependencies(object : Action<DirectDependenciesMetadata> {
                        override fun execute(dependencies: DirectDependenciesMetadata) {
                            val iterator = dependencies.iterator()
                            while (iterator.hasNext()) {
                                val dependency = iterator.next()
                                if (HiroDependencyPolicy.shouldDropComposeMetadataDependency(
                                        dependency.group,
                                        dependency.name,
                                    )
                                ) {
                                    iterator.remove()
                                }
                            }
                        }
                    })
                }
                if (removeRuntimeFiles) {
                    variant.withFiles(object : Action<MutableVariantFilesMetadata> {
                        override fun execute(files: MutableVariantFilesMetadata) {
                            files.removeAllFiles()
                        }
                    })
                }
            }
        }

    private companion object {
        val kotlinPlatformTypeAttribute =
            org.gradle.api.attributes.Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java)
    }
}
