package com.jetbrains.snakecharm.lang.psi

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.WILDCARDS_DEFINING_SECTIONS_KEYWORDS

interface SmkRuleOrCheckpoint : SmkRuleLike<SmkRuleOrCheckpointArgsSection>, PyTypedElement {
    val wildcardsElement: PsiElement
    /**
     * Collect wildcards which are defined at any of defining sections
     */
    fun collectWildcards(): List<String>? {
        val collector = SmkWildcardsCollector(
                visitDefiningSections = true,
                visitExpandingSections = false
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
                .filter { it.isWildcardsDefiningSection() }
                .minBy { WILDCARDS_DEFINING_SECTIONS_KEYWORDS.indexOf(it.sectionKeyword) }
}