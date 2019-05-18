package com.jetbrains.snakecharm.codeInsight

import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.PyTypeProviderBase
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.types.SnakemakeRulesType

class SnakemakeRulesTypeProvider : PyTypeProviderBase() {
    override fun getReferenceExpressionType(
            referenceExpression: PyReferenceExpression,
            context: TypeEvalContext
    ): PyType? {
        if (referenceExpression.containingFile.language != SnakemakeLanguageDialect) {
            return null
        }

        val name = referenceExpression.referencedName
        return if (name == "rules") SnakemakeRulesType() else null
    }
}