package com.jetbrains.snakecharm.lang.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkWrapperDocumentation : AbstractDocumentationProvider() {
    companion object {
        private const val URL_START = "https://bitbucket.org/snakemake/snakemake-wrappers/src/"
        private const val WRAPPER = "wrapper.py"
        private const val META = "meta.yaml"
        private const val ENVIRONMENT = "environment.yaml"
        private const val EXAMPLE_SNAKEFILE = "test/Snakefile"
    }


    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (!originalElement.isStringLiteralInWrapperSection()) {
            return null
        }

        val text = originalElement?.text?.replace("\"", "") ?: return null

        val result = mutableListOf<String>()
        result.add(DocumentationMarkup.DEFINITION_START)
        result.add("Snakemake Wrapper")
        result.add(DocumentationMarkup.DEFINITION_END)
        result.add(DocumentationMarkup.CONTENT_START)
        result.add(text)
        result.add(DocumentationMarkup.CONTENT_END)
        result.add("<p>")

        result.add(DocumentationMarkup.SECTIONS_START)
        result.addAll(buildSectionWithFileLink(text, WRAPPER))
        result.addAll(buildSectionWithFileLink(text, META))
        result.addAll(buildSectionWithFileLink(text, ENVIRONMENT))
        result.addAll(buildSectionWithFileLink(text, EXAMPLE_SNAKEFILE, "Example Snakefile"))
        result.add(DocumentationMarkup.SECTIONS_END)

        val body = "<body>${result.joinToString("")}</body>"
        return "<html>$body</html>"
    }

    override fun getCustomDocumentationElement(editor: Editor, file: PsiFile, contextElement: PsiElement?) =
            if (contextElement.isStringLiteralInWrapperSection()) contextElement else null

    private fun buildSectionWithFileLink(
            partialPath: String,
            filename: String,
            sectionTitle: String = filename
    ): List<String> {
        val wrapperPath = if (partialPath.endsWith("/")) partialPath else "$partialPath/"
        val url = "$URL_START$wrapperPath$filename"
        return listOf(
                DocumentationMarkup.SECTION_HEADER_START,
                sectionTitle,
                DocumentationMarkup.SECTION_SEPARATOR,
                "<p><a href=\"$url\">$url</a>",
                DocumentationMarkup.SECTION_END
        )
    }

    private fun PsiElement?.isStringLiteralInWrapperSection() =
            SnakemakeLanguageDialect.isInsideSmkFile(this) &&
                    this.isInWrapperSection() &&
                    PsiTreeUtil.getParentOfType(this, PyStringLiteralExpression::class.java) != null

    private fun PsiElement?.isInWrapperSection() =
            PsiTreeUtil.getParentOfType(this, SmkRuleOrCheckpointArgsSection::class.java)?.name ==
                    SnakemakeNames.SECTION_WRAPPER
}