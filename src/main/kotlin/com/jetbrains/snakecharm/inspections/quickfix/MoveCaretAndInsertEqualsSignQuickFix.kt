package com.jetbrains.snakecharm.inspections.quickfix

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.snakecharm.SnakemakeBundle

class MoveCaretAndInsertEqualsSignQuickFix(element: PsiElement) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
    override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.name.argument")

    override fun getText() = SnakemakeBundle.message("INSP.INTN.name.argument")

    override fun invoke(
            project: Project,
            file: PsiFile,
            editor: Editor?,
            startElement: PsiElement,
            endElement: PsiElement
    ) {
        val offset = startElement.textOffset
        editor?.caretModel?.moveToOffset(offset)
        if (editor?.document?.isWritable == true) {
            editor.document.insertString(offset, "=")
        }
    }
}