package com.jetbrains.snakecharm.stringLanguage.lang.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import com.jetbrains.snakecharm.lang.psi.SmkSLReferenceExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLReferenceExpressionImpl

class SmkSLHighlightingAnnotator : Annotator {
    override fun annotate(
            element: PsiElement,
            holder: AnnotationHolder) {
        if (element !is SmkSLReferenceExpression) {
            return
        }

        val annotation = when {
            // XXX: Is not fast check
            SmkSLReferenceExpressionImpl.isWildcard(element) -> {
                holder.createAnnotation(
                        HighlightSeverity.INFORMATION,
                        element.textRange,
                        null)
            }
            element.comesAfterWildcardsPrefix() -> {
                holder.createAnnotation(
                        HighlightSeverity.INFORMATION,
                        element.getNameRange(),
                        null)
            }
            else -> null
        }

        annotation?.let { it.textAttributes = PyHighlighter.PY_NUMBER }
    }

    private fun PsiElement.comesAfterWildcardsPrefix(): Boolean {
        val child =
                PsiTreeUtil.getChildOfType(this, SmkSLReferenceExpressionImpl::class.java)
        return child?.name == SnakemakeAPI.SMK_VARS_WILDCARDS && child.children.isEmpty()
    }
}
