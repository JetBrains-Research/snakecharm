package com.jetbrains.snakecharm.lang.psi

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyTypedElement

interface SmkRuleOrCheckpoint : SmkRuleLike<SmkRuleOrCheckpointArgsSection>, PyTypedElement {
    fun collectWildcards(): List<Pair<PsiElement, String>>

    /**
     * Snakemake wildcards can be defined by 3 sections.
     * Defining section priority is: output > log > benchmark
     * @return Section that defines wildcards or null
     *         if there are no such sections
     */
    fun getWildcardDefiningSection() =
        statementList.statements.filter {
            it.name in SmkRuleOrCheckpointArgsSection.KEYWORDS_GENERATING_WILDCARDS
        }.minBy { pyStatement ->
            SmkRuleOrCheckpointArgsSection.KEYWORDS_GENERATING_WILDCARDS.indexOf(pyStatement.name)
        } as SmkRuleOrCheckpointArgsSection?
}