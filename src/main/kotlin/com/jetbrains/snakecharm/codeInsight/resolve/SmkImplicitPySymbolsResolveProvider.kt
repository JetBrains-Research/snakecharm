package com.jetbrains.snakecharm.codeInsight.resolve

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.codeInsight.ImplicitPySymbolsCache
import com.jetbrains.snakecharm.codeInsight.ImplicitPySymbolsProvider
import com.jetbrains.snakecharm.codeInsight.SmkCodeInsightScope
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_VARS_WILDCARDS
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil.RATE_IMPLICIT_SYMBOLS
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames.RUN_SECTION_VARIABLE_JOBID
import com.jetbrains.snakecharm.lang.SnakemakeNames.RUN_SECTION_VARIABLE_RULE
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_THREADS
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint

class SmkImplicitPySymbolsResolveProvider : PyReferenceResolveProvider {
    override fun resolveName(
        element: PyQualifiedExpression,
        context: TypeEvalContext,
    ): List<RatedResolveResult> {
        if (SnakemakeLanguageDialect.isInsideSmkFile(context.origin)) {

            val contextScope = SmkCodeInsightScope[element]
            @Suppress("UnstableApiUsage")
            val referencedName = element.referencedName
            val items = arrayListOf<RatedResolveResult>()

            if (contextScope == SmkCodeInsightScope.RULELIKE_RUN_SECTION) {
                val ruleOrCheckpoint = PsiTreeUtil.getParentOfType(element, SmkRuleOrCheckpoint::class.java)!!

                when (referencedName) {
                    SMK_VARS_WILDCARDS -> {
                        items.add(RatedResolveResult(RATE_IMPLICIT_SYMBOLS, ruleOrCheckpoint.wildcardsElement))
                    }

                    SECTION_THREADS -> {
                        val threadsSection = ruleOrCheckpoint.getSectionByName(SECTION_THREADS)

                        items.add(
                            RatedResolveResult(
                                RATE_IMPLICIT_SYMBOLS, threadsSection ?: ruleOrCheckpoint
                            )
                        )
                    }

                    RUN_SECTION_VARIABLE_RULE -> {
                        items.add(RatedResolveResult(RATE_IMPLICIT_SYMBOLS, ruleOrCheckpoint))
                    }

                    RUN_SECTION_VARIABLE_JOBID -> {
                        items.add(RatedResolveResult(RATE_IMPLICIT_SYMBOLS, null))
                    }
                }
            }

            val cache = ImplicitPySymbolsProvider.instance(element.project).cache

            addSyntheticSymbols(contextScope, cache, referencedName, items)

            SmkCodeInsightScope.entries.asSequence()
                .filter { symbolScope -> contextScope.includes(symbolScope) }
                .flatMap { symbolScope -> cache.filter(symbolScope, element.name!!).asSequence() }
                .map {
                    RatedResolveResult(RATE_IMPLICIT_SYMBOLS, it.psiDeclaration)
                }.forEach {
                    items.add(it)

                }
            return items
        }
        return emptyList()
    }

    companion object {
        fun addSyntheticSymbols(
            contextScope: SmkCodeInsightScope,
            cache: ImplicitPySymbolsCache,
            referencedName: String?,
            items: ArrayList<RatedResolveResult>,
        ) {
            SmkCodeInsightScope.entries.forEach { symbolScope ->
                if (contextScope.includes(symbolScope)) {
                    for (l in cache.getSynthetic(symbolScope)) {
                        // TODO: Introduce ImplicitLookupItem class
                        val psi = l.psiElement
                        if (l.lookupString == referencedName && psi != null && psi.isValid) {
                            items.add(RatedResolveResult(RATE_IMPLICIT_SYMBOLS, psi))
                            break
                        }
                    }
                }
            }
        }

    }
}
