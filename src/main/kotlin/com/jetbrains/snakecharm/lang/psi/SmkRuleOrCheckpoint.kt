package com.jetbrains.snakecharm.lang.psi

import com.jetbrains.python.psi.PyTypedElement

interface SmkRuleOrCheckpoint : SmkRuleLike<SmkRuleOrCheckpointArgsSection>, PyTypedElement {
    /**
     * Collect wildcards which are defined at any of defining sections
     */
    fun collectWildcards(): List<String>? {
        val collector = SmkWildcardsCollector(
                visitDefiningSections = true,
                visitSectionsAllowingUsage = false
        )
        this.accept(collector)
        return collector.getWildcardsNames()
    }

    /**
     * Snakemake wildcards can be defined by 3 sections.
     * Defining section priority is: output > log > benchmark
     * @return Section that defines wildcards or null
     *         if there are no such sections
     */
    fun getWildcardDefiningSection() =
        statementList.statements
                .filterIsInstance<SmkRuleOrCheckpointArgsSection>()
                .filter { it.sectionKeyword in SmkRuleOrCheckpointArgsSection.SECTIONS_DEFINING_WILDCARDS }
                .minBy { SmkRuleOrCheckpointArgsSection.SECTIONS_DEFINING_WILDCARDS.indexOf(it.sectionKeyword) }
}