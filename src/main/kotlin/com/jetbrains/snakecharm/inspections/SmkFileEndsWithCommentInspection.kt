package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.nextLeaf
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkFile

class SmkFileEndsWithCommentInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

        override fun visitComment(comment: PsiComment) {
            if(comment.containingFile !is SmkFile) {
                return
            }

            val nextLeaf = comment.nextLeaf(true)
            if (nextLeaf == null) {
                registerProblem(
                    comment,
                    SnakemakeBundle.message("INSP.NAME.file.ends.with.comment.message"),
                    InsertEmptyLine(comment)
                )
            }
        }
    }
}

class InsertEmptyLine(expr: PsiElement) : LocalQuickFixOnPsiElement(expr) {
    override fun getFamilyName() = SnakemakeBundle.message("INSP.NAME.file.ends.with.comment.quick.fix")

    override fun getText() = familyName

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        val doc = PsiDocumentManager.getInstance(project).getDocument(file)

        val endOffsetOffset = endElement.textOffset + endElement.textLength
        doc!!.insertString(endOffsetOffset, "\n")

        PsiDocumentManager.getInstance(project).commitDocument(doc)
    }
}