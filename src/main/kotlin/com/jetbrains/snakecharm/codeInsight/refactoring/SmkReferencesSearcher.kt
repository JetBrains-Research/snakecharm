package com.jetbrains.snakecharm.codeInsight.refactoring

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SingleTargetRequestResultProcessor
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch.SearchParameters
import com.intellij.util.Processor
import com.jetbrains.python.psi.PyClass
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_VARS_WILDCARDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.WILDCARDS_ACCESSOR_CLASS

/**
 * Register:
 * <referencesSearch implementation="com.jetbrains.snakecharm.codeInsight.refactoring.SmkReferencesSearcher"/>
 *
 * TODO: cleanup we implemented 'wildcards' support using
 */
class SmkReferencesSearcher : QueryExecutorBase<PsiReference, SearchParameters>(true) {
    override fun processQuery(queryParameters: SearchParameters, consumer: Processor<in PsiReference>) {
        val element = queryParameters.elementToSearch
        if (element is PyClass) {
            val fqn = element.qualifiedName
            if (WILDCARDS_ACCESSOR_CLASS == fqn) {
                val searchScope = queryParameters.effectiveSearchScope
                val processor = SingleTargetRequestResultProcessor(element)

                val searchContext = UsageSearchContext.IN_CODE
                queryParameters.optimizer.searchWord(
                        SMK_VARS_WILDCARDS, searchScope, searchContext, true, element, processor
                )
            }

        }

    }
}