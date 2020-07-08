package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.elementType
import com.intellij.psi.util.forEachDescendantOfType
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.snakecharm.SmkFileType

import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.inspections.quickfix.RenameElementWithoutUsagesQuickFix
import com.jetbrains.snakecharm.lang.psi.SmkSection
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes
import com.jetbrains.snakecharm.stringLanguage.lang.parser.SmkSLTokenTypes

class SmkRedundantComaInspection : SnakemakeInspection() {
    override fun buildVisitor(
                              holder: ProblemsHolder,
                              isOnTheFly: Boolean,
                              session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitPyArgumentList(node: PyArgumentList) {
            if (node.lastChild?.elementType !== PyTokenTypes.COMMA
                    || node.parent !is SmkSection) return
            holder.registerProblem(
                    node.lastChild,
                    SnakemakeBundle.message("INSP.NAME.redundant.coma.title"),
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    fix
            )
        }
    }


    private class RemoveSectionQuickFix : LocalQuickFix {
        override fun getFamilyName() = SnakemakeBundle.message("INSP.NAME.redundant.coma.fix.message")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) { descriptor.psiElement.delete() }
        }
    }

    companion object {
        private val fix = RemoveSectionQuickFix()
    }
}