package com.jetbrains.snakecharm.codeInsight.resolve

import com.intellij.openapi.module.ModuleUtilCore
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.codeInsight.ImplicitPySymbolsCache
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect

class SMKImplicitPySymbolsResolveProvider : PyReferenceResolveProvider {
    override fun resolveName(
            element: PyQualifiedExpression,
            context: TypeEvalContext
    ): List<RatedResolveResult> {
        if (context.origin?.language == SnakemakeLanguageDialect) {
            val module = ModuleUtilCore.findModuleForPsiElement(element)
            if (module != null) {
                val elements = ImplicitPySymbolsCache.instance(module).find(element.name!!)
                if (elements.isNotEmpty()) {
                    return elements.map { RatedResolveResult(RatedResolveResult.RATE_NORMAL, it) }
                }
            }
        }
        return emptyList()
    }
}
