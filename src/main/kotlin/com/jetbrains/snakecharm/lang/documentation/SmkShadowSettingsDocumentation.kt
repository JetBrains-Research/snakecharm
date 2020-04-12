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
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection


class SmkShadowSettingsDocumentation : AbstractDocumentationProvider() {
    override fun generateDoc(
            element: PsiElement,
            originalElement: PsiElement?): String? {
        return if (element.isStringLiteralInShadowSection()) getDocumentation(element) else null
    }

    private fun getDocumentation(element: PsiElement): String? {
        val stringLiteral = PsiTreeUtil.getParentOfType(element, PyStringLiteralExpression::class.java)!!
        return when (stringLiteral.stringValue) {
            "full" -> SnakemakeBundle.message("INSP.NAME.shadow.settings.full")
            "shallow" -> SnakemakeBundle.message("INSP.NAME.shadow.settings.shallow")
            "minimal" -> SnakemakeBundle.message("INSP.NAME.shadow.settings.minimal")
            else -> null
        }
    }

    override fun getCustomDocumentationElement(
        editor: Editor,
        file: PsiFile,
        contextElement: PsiElement?,
        targetOffset: Int
    ) = when {
        contextElement.isStringLiteralInShadowSection() -> contextElement
        else -> null
    }

    private fun PsiElement?.isStringLiteralInShadowSection() =
            SnakemakeLanguageDialect.isInsideSmkFile(this) &&
                    this.isInShadowSection() &&
                    PsiTreeUtil.getParentOfType(this, PyStringLiteralExpression::class.java) != null

    private fun PsiElement?.isInShadowSection() =
            PsiTreeUtil.getParentOfType(this, SmkRuleOrCheckpointArgsSection::class.java)?.name ==
                    SnakemakeNames.SECTION_SHADOW
}