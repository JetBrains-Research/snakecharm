package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint

/**
 * Not thread safe implementation
 */
abstract class AbstractSmkWildcardsInspectionVisitor<T>(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession
) : SnakemakeInspectionVisitor(holder, session) {
    private var cachedDeclarationAndWildcards: Pair<SmkRuleOrCheckpoint?, List<String>>? = null

    fun collectWildcards(lazyRuleLikeProvider: () -> SmkRuleOrCheckpoint?): Pair<SmkRuleOrCheckpoint?, List<String>> {
        initWildcards(lazyRuleLikeProvider)

        return cachedDeclarationAndWildcards!!
    }

    private fun initWildcards(lazyRuleLikeProvider: () -> SmkRuleOrCheckpoint?) {
        if (cachedDeclarationAndWildcards == null) {
            val ruleLike = lazyRuleLikeProvider()
            val wildcards = ruleLike?.collectWildcards()?.map { it.second } ?: emptyList()
            cachedDeclarationAndWildcards = ruleLike to wildcards
        }
    }
}