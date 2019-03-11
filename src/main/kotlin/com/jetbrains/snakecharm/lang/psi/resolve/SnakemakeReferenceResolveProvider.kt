package com.jetbrains.snakecharm.lang.psi.resolve

import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.PyPsiFacade
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext

class SnakemakeReferenceResolveProvider : PyReferenceResolveProvider {
    private val namesToFQN = hashMapOf("expand" to "expand") // TODO what is the qualified name of 'expand'?

    override fun resolveName(element: PyQualifiedExpression, context: TypeEvalContext): MutableList<RatedResolveResult> {
        val facade = PyPsiFacade.getInstance(element.project)
        val fullyQualifiedName = namesToFQN[element.name] ?: return mutableListOf()
        val qName = QualifiedName.fromDottedString(fullyQualifiedName)

        val resolveContext = facade.createResolveContextFromFoothold(element)
        val resolvedModules = facade.resolveQualifiedName(qName, resolveContext)

        val result = mutableListOf<RatedResolveResult>()
        result.addAll(resolvedModules.map { RatedResolveResult(RatedResolveResult.RATE_NORMAL, it) })

        return result
    }
}