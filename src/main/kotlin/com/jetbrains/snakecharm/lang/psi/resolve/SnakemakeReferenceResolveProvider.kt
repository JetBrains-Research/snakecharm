package com.jetbrains.snakecharm.lang.psi.resolve

import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext

class SnakemakeReferenceResolveProvider : PyReferenceResolveProvider {
    private val namesToFQN = hashMapOf("expand" to "snakemake.io")

    override fun resolveName(element: PyQualifiedExpression, context: TypeEvalContext): List<RatedResolveResult> {
        val facade = PyPsiFacade.getInstance(element.project)

        val fullyQualifiedName = namesToFQN[element.name] ?: return emptyList()
        val qName = QualifiedName.fromDottedString(fullyQualifiedName)

        val resolveContext = facade.createResolveContextFromFoothold(element).copyWithMembers()

        return facade.resolveQualifiedName(qName, resolveContext)
                .filterIsInstance<PyFile>()
                .firstOrNull()?.multiResolveName(element.name!!) ?: emptyList()
    }
}