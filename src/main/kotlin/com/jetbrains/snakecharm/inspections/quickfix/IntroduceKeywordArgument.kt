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
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.snakecharm.SnakemakeBundle

class IntroduceKeywordArgument(element: PyExpression) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
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
        if (editor == null || !startElement.isValid) {
            return
        }
        val elementGenerator = PyElementGenerator.getInstance(project)
        val argument = elementGenerator.createKeywordArgument(
                LanguageLevel.forElement(startElement),
                defaultArgumentName,
                startElement.text
        )

        val newElement = startElement.replace(argument)?.let {
            CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(it)
        } ?: return

        val builder = TemplateBuilderFactory.getInstance().createTemplateBuilder(newElement)
        @Suppress("UnstableApiUsage")
        val keywordArgument = (newElement as PyKeywordArgument).keywordNode!!.psi
        builder.replaceElement(keywordArgument, defaultArgumentName)
        builder.run(editor, false)
    }
}