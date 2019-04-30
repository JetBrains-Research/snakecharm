package com.jetbrains.snakecharm.lang.psi.resolve

import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyPsiFacade
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.resolve.PyReferenceResolveProvider
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect

class SnakemakeReferenceResolveProvider : PyReferenceResolveProvider {
    private val namesToFQN = hashMapOf("expand" to "snakemake.io")

    override fun resolveName(element: PyQualifiedExpression, context: TypeEvalContext): List<RatedResolveResult> {
        if (context.origin?.language != SnakemakeLanguageDialect) {
            return emptyList()
        }

        val facade = PyPsiFacade.getInstance(element.project)

        val fullyQualifiedName = namesToFQN[element.name] ?: return emptyList()
        val qName = QualifiedName.fromDottedString(fullyQualifiedName)

        val resolveContext = facade.createResolveContextFromFoothold(element).copyWithMembers()

        return facade.resolveQualifiedName(qName, resolveContext)
                .filterIsInstance<PyFile>()
                .firstOrNull()?.multiResolveName(element.name!!) ?: emptyList()
    }
}