package com.jetbrains.snakecharm.lang.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement


class SmkShadowSettingsDocumentation : AbstractDocumentationProvider() {
    override fun generateDoc(
            element: PsiElement,
            originalElement: PsiElement?): String? {
        if (!SnakemakeLanguageDialect.isInsideSmkFile(element) ||
            !element.isInShadowSection() || element.parent !is PyStringLiteralExpression) {
            return null
        }

        return getDocumentation(element)
    }

    private fun getDocumentation(element: PsiElement): String? =
            when (element.text) {
                "\"full\"" -> SnakemakeBundle.message("INSP.NAME.shadow.settings.full")
                "\"shallow\"" -> SnakemakeBundle.message("INSP.NAME.shadow.settings.shallow")
                "\"minimal\"" -> SnakemakeBundle.message("INSP.NAME.shadow.settings.minimal")
                else -> null
            }

    override fun getCustomDocumentationElement(
            editor: Editor,
            file: PsiFile,
            contextElement: PsiElement?): PsiElement? {
        return contextElement
    }
  
    private fun PsiElement.isInShadowSection(): Boolean =
            PsiTreeUtil.getParentOfType(this, SMKRuleParameterListStatement::class.java)?.name ==
                    SMKRuleParameterListStatement.SHADOW
}