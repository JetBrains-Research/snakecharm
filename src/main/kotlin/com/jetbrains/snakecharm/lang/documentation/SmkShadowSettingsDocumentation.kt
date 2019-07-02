package com.jetbrains.snakecharm.lang.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement


class SmkShadowSettingsDocumentation : AbstractDocumentationProvider() {
    override fun generateDoc(
            element: PsiElement?,
            originalElement: PsiElement?): String? {
        if (!element.isInShadowSection() || element?.parent !is PyStringLiteralExpression) {
            return null
        }

        return getDocumentation(element)
    }

    private fun getDocumentation(element: PsiElement) : String? =
            when (element.text) {
                "\"full\"" -> "The setting shadow: \"full\" fully shadows the entire subdirectory structure of the current workdir."
                "\"shallow\"" -> "By setting shadow: \"shallow\", the top level files and directories are symlinked," +
                        "so that any relative paths in a subdirectory will be real paths in the filesystem."
                "\"minimal\"" -> "The setting shadow: \"minimal\" only symlinks the inputs to the rule."
                else -> null
            }

    override fun getCustomDocumentationElement(
            editor: Editor,
            file: PsiFile,
            contextElement: PsiElement?): PsiElement? {
        return contextElement
    }
}

fun PsiElement?.isInShadowSection() : Boolean =
        PsiTreeUtil.getParentOfType(this, SMKRuleParameterListStatement::class.java)?.name ==
                SMKRuleParameterListStatement.SHADOW