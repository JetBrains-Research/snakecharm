package com.jetbrains.snakecharm.stringLanguage.lang.highlighter

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbService
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.python.validation.PyAnnotationHolder
import com.jetbrains.snakecharm.lang.psi.types.SmkWildcardsType
import com.jetbrains.snakecharm.stringLanguage.lang.highlighter.SmkSLSyntaxHighlighter.Companion.HIGHLIGHTING_WILDCARDS_KEY
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpression

class SmkSLWildcardsAnnotator(holder: PyAnnotationHolder) : AbstractSmkSLAnnotator(holder) {

    override fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpression) {
        val exprIdentifier = expr.nameIdentifier

        @Suppress("UnstableApiUsage")
        when {
            expr.isWildcard() -> {
                addHighlightingAnnotation(
                        expr, HIGHLIGHTING_WILDCARDS_KEY, HighlightSeverity.INFORMATION
                )
            }

            exprIdentifier != null -> {
                val qualifier = expr.qualifier
                if (qualifier != null && !DumbService.isDumb(expr.project)) {
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