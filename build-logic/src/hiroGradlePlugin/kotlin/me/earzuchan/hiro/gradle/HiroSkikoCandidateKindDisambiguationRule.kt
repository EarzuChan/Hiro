package me.earzuchan.hiro.gradle

import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.attributes.MultipleCandidatesDetails

abstract class HiroSkikoCandidateKindDisambiguationRule : AttributeDisambiguationRule<String> {
    override fun execute(details: MultipleCandidatesDetails<String>) {
        HiroSkikoCandidateKind.priority.firstOrNull { candidate ->
            candidate in details.candidateValues
        }?.let(details::closestMatch)
    }
}
