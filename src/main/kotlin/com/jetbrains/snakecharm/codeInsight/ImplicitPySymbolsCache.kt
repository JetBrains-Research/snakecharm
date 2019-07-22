package com.jetbrains.snakecharm.codeInsight

import com.jetbrains.python.psi.PyElement

/**
 * @author Roman.Chernyatchik
 * @date 2019-05-07
 */
interface ImplicitPySymbolsCache {
    val contentVersion: Int
    operator fun get(scope: SmkCodeInsightScope): List<ImplicitPySymbol>

    fun filter(smkScope: SmkCodeInsightScope, name: String) = this[smkScope]
            .asSequence()
            .filter { symbol -> symbol.identifier == name }
            .toList()

    companion object {
        fun emptyCache() = object : ImplicitPySymbolsCache {
            override val contentVersion = 0
            override fun get(scope: SmkCodeInsightScope) = emptyList<ImplicitPySymbol>()
        }
    }
}

data class ImplicitPySymbol(
        val identifier: String,
        val psiDeclaration: PyElement,
        val scope: SmkCodeInsightScope
)
