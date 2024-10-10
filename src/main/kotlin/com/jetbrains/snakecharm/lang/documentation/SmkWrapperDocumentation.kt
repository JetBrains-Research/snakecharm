package com.jetbrains.snakecharm.lang.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
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
        val text = node.stringValue
        val result = if (text.startsWith("file://")) {
            text.substringAfter("file://")
        } else {
            text.substringAfter("/")
        }
        val wrappers = SmkWrapperStorage.getInstance(node.navigationElement.project).wrappers
        val filterWrappers =  wrappers.filter { wrapper -> wrapper.path == result }
        require(filterWrappers.size <= 1) { "Found multiple wrappers (${filterWrappers.size}) for path: $result" }
        val wrapper = filterWrappers.firstOrNull()
        require(wrapper == null || wrapper.path == result) { "Found wrapper with different path: ${wrapper!!.path} != $result" }
        if (text.startsWith("file://")) {
            return if (wrapper != null)
                """
                <p><a href="$text">Folder</a></p>
                ${wrapper.description.lineSequence().map { "<p>$it</p>"  }.joinToString(System.lineSeparator())}
                """.trimIndent()
            else
                """
                <p><a href="$text">Folder</a></p>
                """.trimIndent()
        } else {
            val urlDocs = "https://snakemake-wrappers.readthedocs.io/en/${text}"
                    .replace("/master/", "/latest/")
                    .replace("/bio/", "/wrappers/")
                    .replace("/utils/", "/wrappers/") + ".html"
            val urlCode = "https://github.com/snakemake/snakemake-wrappers/tree/${text}"
                    .replace("/latest/", "/master/")
            return if (wrapper != null)
                """
                <p><a href="$urlDocs">Documentation</a>, <a href="$urlCode">Source Code</a></p>
                ${wrapper.description.lineSequence().map { "<p>$it</p>"  }.joinToString(System.lineSeparator())}
                """.trimIndent()
            else
                """
                <p><a href="$urlDocs">Documentation</a>, <a href="$urlCode">Source Code</a></p>
                """.trimIndent()
        }
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
