package me.earzuchan.hiro.gradleplugin

import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.attributes.MultipleCandidatesDetails

abstract class HiroKmpVariantKindDisambiguationRule : AttributeDisambiguationRule<String> {
    override fun execute(details: MultipleCandidatesDetails<String>) {
        HiroKmpVariantKind.priority.firstOrNull { candidate -> candidate.wireName in details.candidateValues }?.let { candidate -> details.closestMatch(candidate.wireName) }
    }
}
