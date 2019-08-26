package com.jetbrains.snakecharm.stringLanguage.lang.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkSLReferenceExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLReferenceExpressionImpl

class SmkSLHighlightingAnnotator : Annotator {
    override fun annotate(
            element: PsiElement,
            holder: AnnotationHolder) {
        if (element !is SmkSLReferenceExpressionImpl) {
            return
        }

        val annotation = when {
            element.isWildcard() -> {
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
        return child?.name == SnakemakeNames.SMK_VARS_WILDCARDS && child.children.isEmpty()
    }


    private fun PsiElement.isWildcard() =
            PsiTreeUtil.getParentOfType(
                            this,
                            SmkSLReferenceExpression::class.java
                    ) == null &&
                    isInValidSection() &&
                    text.isNotEmpty()

    private fun PsiElement.isInValidSection(): Boolean {
        val languageManager = InjectedLanguageManager.getInstance(project)
        val host = languageManager.getInjectionHost(this)
        val name = PsiTreeUtil.getParentOfType(host, SmkRuleOrCheckpointArgsSection::class.java)?.name

        return name in SmkRuleOrCheckpointArgsSection.KEYWORDS_CONTAINING_WILDCARDS
    }
}
