package com.jetbrains.snakecharm.lang.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.codeInsight.completion.wrapper.SmkWrapperStorage
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkWrapperDocumentation : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement, originalElement: PsiElement?): String? {
        return if (element.isStringLiteralInWrapperSection()) {
            getDocumentation(element)
        } else {
            null
        }
    }

    private fun getDocumentation(element: PsiElement): String {
        val stringLiteral = PsiTreeUtil.getParentOfType(element, PyStringLiteralExpression::class.java)!!
        return processUrl(stringLiteral)
    }

    private fun processUrl(node: PyStringLiteralExpression): String {
        val result = node.text.trim('"').substringAfter("/")
        val wrappers = node.project.service<SmkWrapperStorage>().wrapperStorage
        val wrapper =  wrappers.find { wrapper -> wrapper.path.contains(result) }
        val url = "https://snakemake-wrappers.readthedocs.io/en/" +
                node.stringValue
                .replace("/bio/", "/wrappers/")
                .replace("/utils/", "/wrappers/") + ".html"
        return if (wrapper != null)
            """
            <p>${wrapper.name}</p>
            <p>${wrapper.author}</p>
            <p>${wrapper.description}</p>
            <p><a href="$url">$url</a></p>
            """
        else
            "<p><a href=\"$url\">$url</a></p>"
    }

    override fun getCustomDocumentationElement(
            editor: Editor,
            file: PsiFile,
            contextElement: PsiElement?,
            targetOffset: Int
    ) = when {
        contextElement.isStringLiteralInWrapperSection() -> contextElement
        else -> null
    }

    private fun PsiElement?.isStringLiteralInWrapperSection() =
            SnakemakeLanguageDialect.isInsideSmkFile(this) &&
                    this.isInWrapperSection() &&
                    PsiTreeUtil.getParentOfType(this, PyStringLiteralExpression::class.java) != null

    private fun PsiElement?.isInWrapperSection() =
            PsiTreeUtil.getParentOfType(this, SmkRuleOrCheckpointArgsSection::class.java)?.name ==
                    SnakemakeNames.SECTION_WRAPPER
}
