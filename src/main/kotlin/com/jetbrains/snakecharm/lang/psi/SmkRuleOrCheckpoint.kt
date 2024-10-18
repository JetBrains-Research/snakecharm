package com.jetbrains.snakecharm.lang.psi

import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.snakecharm.codeInsight.SnakemakeApi.WILDCARDS_DEFINING_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.lang.psi.impl.SmkWildcardFakePsiElement

interface SmkRuleOrCheckpoint : SmkRuleLike<SmkRuleOrCheckpointArgsSection>,
        //consider PyQualifiedNameOwner,
        PyTypedElement {
    val wildcardsElement: SmkWildcardFakePsiElement

    /**
     * Snakemake wildcards can be defined by 3 sections.
     * Defining section priority is: output > log > benchmark
     * @return Section that defines wildcards or null
     *         if there are no such sections
     */
    fun getWildcardDefiningSection() =
        statementList.statements
                .filterIsInstance<SmkRuleOrCheckpointArgsSection>()
                .filter { it.isWildcardsDefiningSection }
                .minByOrNull { WILDCARDS_DEFINING_SECTIONS_KEYWORDS.indexOf(it.sectionKeyword) }
}