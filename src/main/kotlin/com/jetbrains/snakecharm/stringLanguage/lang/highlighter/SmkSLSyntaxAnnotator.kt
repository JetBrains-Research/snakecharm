package com.jetbrains.snakecharm.stringLanguage.lang.highlighter

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import com.jetbrains.snakecharm.lang.psi.SmkSLReferenceExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLReferenceExpressionImpl

object SmkSLSyntaxAnnotator : AbstractSmkSLAnnotator() {
    override fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpression) {
        val wildcard = SmkSLReferenceExpressionImpl.isWildcard(expr)
        when {
            // XXX: Is not fast check
            wildcard -> {
                addHighlightingAnnotation(
                        expr, PyHighlighter.PY_NUMBER, HighlightSeverity.INFORMATION
                )
            }

            // TODO: replace 'wildcards.' check with: if left part of reference
            //  is 'wildcards' with "WildcardsType" (Smk + SmkSL)
            expr.comesAfterWildcardsPrefix() -> {
                // expr.getNameRange()
                addHighlightingAnnotation(
                        expr.getNameNode()?.psi!!, PyHighlighter.PY_NUMBER, HighlightSeverity.INFORMATION
                )
            }
        }
    }

    private fun PsiElement.comesAfterWildcardsPrefix(): Boolean {
        val child =
                PsiTreeUtil.getChildOfType(this, SmkSLReferenceExpressionImpl::class.java)
        return child?.name == SnakemakeAPI.SMK_VARS_WILDCARDS && child.children.isEmpty()
    }
}