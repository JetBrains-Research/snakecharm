package com.jetbrains.snakecharm.codeInsight

import com.intellij.codeInsight.lookup.LookupElement
import com.jetbrains.python.psi.PyElement

/**
 * @author Roman.Chernyatchik
 * @date 2019-05-07
 */
interface ImplicitPySymbolsCache {
    val contentVersion: Int

    /**
     * @return Symbols based on psi elements in code which are inserted on runtime in snakemake execution context
     */
    operator fun get(scope: SmkCodeInsightScope): List<ImplicitPySymbol>

    /**
     * @return Completion elements that doesn't have PSI definition in snakemake files but also are dynamically
     *  inserted in execution context at runtime, e.g. global variables of workflow (rules, config, ..). The could be
     *  resolved to some PSI elements (e.g. related classes, etc) which cannot be used as definitions in code insight engine.
     */
    fun getSynthetic(scope: SmkCodeInsightScope) : List<LookupElement>

    fun filter(smkScope: SmkCodeInsightScope, name: String) = this[smkScope]
            .asSequence()
            .filter { symbol -> symbol.identifier == name }
            .toList()

    fun contains(smkScope: SmkCodeInsightScope, name: String) = this[smkScope]
            .any { symbol -> symbol.identifier == name }


    companion object {
        fun emptyCache() = object : ImplicitPySymbolsCache {
            override val contentVersion = 0
            override fun get(scope: SmkCodeInsightScope) = emptyList<ImplicitPySymbol>()
            override fun getSynthetic(scope: SmkCodeInsightScope) = emptyList<LookupElement>()
        }
    }
}

data class ImplicitPySymbol(
        val identifier: String,
        val psiDeclaration: PyElement,
        val scope: SmkCodeInsightScope,
        val usageType: ImplicitPySymbolUsageType
)
enum class ImplicitPySymbolUsageType {
    VARIABLE,
    METHOD,
}