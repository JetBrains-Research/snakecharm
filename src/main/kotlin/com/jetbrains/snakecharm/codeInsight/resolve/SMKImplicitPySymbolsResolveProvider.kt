package com.jetbrains.snakecharm.codeInsight.resolve

import com.intellij.openapi.module.ModuleUtilCore
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.codeInsight.ImplicitPySymbolsProvider
import com.jetbrains.snakecharm.codeInsight.SmkCodeInsightScope
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect

class SMKImplicitPySymbolsResolveProvider : PyReferenceResolveProvider {
    override fun resolveName(
            element: PyQualifiedExpression,
            context: TypeEvalContext
    ): List<RatedResolveResult> {
        if (SnakemakeLanguageDialect.isInsideSmkFile(context.origin)) {
            val module = ModuleUtilCore.findModuleForPsiElement(element)
            if (module != null) {
                val contextScope = SmkCodeInsightScope[element]
                val cache = ImplicitPySymbolsProvider.instance(module).cache

                return SmkCodeInsightScope.values().asSequence()
                        .filter { symbolScope -> contextScope.includes(symbolScope) }
                        .flatMap { symbolScope -> cache.filter(symbolScope, element.name!!).asSequence() }
                        .map {
                            RatedResolveResult(RatedResolveResult.RATE_NORMAL, it.psiDeclaration)
                        }.toList()
            }
        }
        return emptyList()
    }
}
