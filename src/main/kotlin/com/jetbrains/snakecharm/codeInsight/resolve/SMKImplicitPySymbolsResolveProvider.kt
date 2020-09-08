package com.jetbrains.snakecharm.codeInsight.resolve

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.codeInsight.ImplicitPySymbolsProvider
import com.jetbrains.snakecharm.codeInsight.SmkCodeInsightScope
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_VARS_WILDCARDS
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil.RATE_IMPLICIT_SYMBOLS
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames.RUN_SECTION_VARIABLE_JOBID
import com.jetbrains.snakecharm.lang.SnakemakeNames.RUN_SECTION_VARIABLE_RULE
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_THREADS
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint

class SMKImplicitPySymbolsResolveProvider : PyReferenceResolveProvider {
    override fun resolveName(
            element: PyQualifiedExpression,
            context: TypeEvalContext
    ): List<RatedResolveResult> {
        if (SnakemakeLanguageDialect.isInsideSmkFile(context.origin)) {

            val contextScope = SmkCodeInsightScope[element]
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
                                    RATE_IMPLICIT_SYMBOLS, threadsSection ?: ruleOrCheckpoint)
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

            val module = ModuleUtilCore.findModuleForPsiElement(element)
            if (module != null) {
                val cache = ImplicitPySymbolsProvider.instance(module).cache

                SmkCodeInsightScope.values().asSequence()
                        .filter { symbolScope -> contextScope.includes(symbolScope) }
                        .flatMap { symbolScope -> cache.filter(symbolScope, element.name!!).asSequence() }
                        .map {
                            RatedResolveResult(RATE_IMPLICIT_SYMBOLS, it.psiDeclaration)
                        }.forEach {
                            items.add(it)

                        }
            }
            return items
        }
        return emptyList()
    }
}
