package com.jetbrains.snakecharm.lang.highlighter

import com.intellij.lang.annotation.HighlightSeverity
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil
import com.jetbrains.snakecharm.lang.psi.types.SmkWildcardsType
import com.jetbrains.snakecharm.lang.validation.SmkAnnotator
import com.jetbrains.snakecharm.stringLanguage.lang.highlighter.SmkSLWildcardsAnnotator.HIGHLIGHTING_WILDCARDS_KEY

object SmkWildcardsAnnotator : SmkAnnotator() {
    override fun visitPyReferenceExpression(expr: PyReferenceExpression) {
        if (!SmkPsiUtil.isInsideSnakemakeOrSmkSLFile(expr)) {
            return
        }

        val exprIdentifier = expr.nameElement?.psi ?: return

        val qualifier = expr.qualifier
        if (qualifier != null) {
            val qtype = TypeEvalContext.codeAnalysis(expr.project, expr.containingFile).getType(qualifier)
            if (qtype is SmkWildcardsType) {
                addHighlightingAnnotation(
                        exprIdentifier, HIGHLIGHTING_WILDCARDS_KEY, HighlightSeverity.INFORMATION
                )
            }
        }
    }
}