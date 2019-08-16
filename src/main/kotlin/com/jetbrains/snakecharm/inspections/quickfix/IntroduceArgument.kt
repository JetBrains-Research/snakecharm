package com.jetbrains.snakecharm.inspections.quickfix

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.codeInsight.template.TemplateBuilderFactory
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.impl.PyPsiUtils
import com.jetbrains.snakecharm.SnakemakeBundle

class IntroduceArgument(element: PsiElement) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
    private val defaultArgumentName = "arg"

    override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.name.argument")

    override fun getText() = SnakemakeBundle.message("INSP.INTN.name.argument")

    override fun invoke(
            project: Project,
            file: PsiFile,
            editor: Editor?,
            startElement: PsiElement,
            endElement: PsiElement
    ) {
        var element = startElement
        PyPsiUtils.assertValid(element)
        if (element.isValid) {
            val elementGenerator = PyElementGenerator.getInstance(project)
            val argument = elementGenerator.createKeywordArgument(
                    LanguageLevel.forElement(element),
                    defaultArgumentName,
                    element.text
            )

            element = element.replace(argument)
            if (element == null) return
            element = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(element)
            if (element == null) return
            val builder = TemplateBuilderFactory.getInstance().createTemplateBuilder(element)
            val keywordArgument = (element as PyKeywordArgument).keywordNode?.psi!!
            builder.replaceElement(keywordArgument, defaultArgumentName)
            builder.run(editor!!, false)
        }
    }
}