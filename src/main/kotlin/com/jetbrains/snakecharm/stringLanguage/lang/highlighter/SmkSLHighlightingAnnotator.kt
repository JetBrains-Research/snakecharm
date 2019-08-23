package com.jetbrains.snakecharm.stringLanguage.lang.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkSLReferenceExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLReferenceExpressionImpl

class SmkSLHighlightingAnnotator : Annotator {
    override fun annotate(
            element: PsiElement,
            holder: AnnotationHolder) {
        if (!element.isWildcard()) {
            return
        }

        val annotation = holder.createAnnotation(
                HighlightSeverity.INFORMATION,
                element.textRange,
                null)

        annotation.textAttributes = PyHighlighter.PY_NUMBER
    }

    private fun PsiElement.isWildcard(): Boolean =
            this is SmkSLReferenceExpressionImpl &&
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
