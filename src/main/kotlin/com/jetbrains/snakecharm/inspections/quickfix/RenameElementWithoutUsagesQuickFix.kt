package com.jetbrains.snakecharm.inspections.quickfix

import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.jetbrains.snakecharm.SnakemakeBundle

class RenameElementWithoutUsagesQuickFix(
        element: PsiElement,
        private val renameStartOffset: Int = element.textRange.startOffset,
        private val renameEndOffset: Int = element.textRange.endOffset
) :
        LocalQuickFixAndIntentionActionOnPsiElement(element) {
    private val defaultName = "SNAKEMAKE_IDENTIFIER"

    override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.rename.element")

    override fun getText() = SnakemakeBundle.message("INSP.INTN.rename.element")

    override fun invoke(
            project: Project,
            file: PsiFile,
            editor: Editor?,
            startElement: PsiElement,
            endElement: PsiElement
    ) {
        val virtualFile = startElement.containingFile.virtualFile
        if (virtualFile != null) {
            val builder = TemplateBuilderImpl(startElement)
            val replacementString =
                    when {
                        ApplicationManager.getApplication().isUnitTestMode -> defaultName
                        startElement is PsiNamedElement -> startElement.name!!
                        else -> startElement.text.substring(renameStartOffset, renameEndOffset)
                    }
            assert(editor != null)
            builder.replaceElement(
                    startElement,
                    TextRange.create(renameStartOffset, renameEndOffset),
                    replacementString
            )
            builder.run(editor!!, false)
        }
    }
}