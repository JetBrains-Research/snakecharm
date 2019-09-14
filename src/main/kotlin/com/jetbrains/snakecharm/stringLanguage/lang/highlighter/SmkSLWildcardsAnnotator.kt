package com.jetbrains.snakecharm.stringLanguage.lang.highlighter

import com.intellij.lang.annotation.HighlightSeverity
import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.types.SmkWildcardsType
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpressionImpl
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLWildcardReference

object SmkSLWildcardsAnnotator : AbstractSmkSLAnnotator() {
    val HIGHLIGHTING_WILDCARDS_KEY = PyHighlighter.PY_NUMBER!!

    override fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpressionImpl) {
        val exprIdentifier = expr.nameIdentifier

        when {
            expr.reference is SmkSLWildcardReference -> {
                addHighlightingAnnotation(
                        expr, HIGHLIGHTING_WILDCARDS_KEY, HighlightSeverity.INFORMATION
                )
            }

            exprIdentifier != null -> {
                val qualifier = expr.qualifier
                if (qualifier != null) {
                    val type = TypeEvalContext.codeAnalysis(expr.project, expr.containingFile).getType(qualifier)
                    if (type is SmkWildcardsType) {
                        addHighlightingAnnotation(
                                exprIdentifier, HIGHLIGHTING_WILDCARDS_KEY, HighlightSeverity.INFORMATION
                        )
                    }
                }
            }
        }
    }
}