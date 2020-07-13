package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.TokenType
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parents
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkRedundantComaInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        fun problemRegiser(psiEl: PsiElement) = holder.registerProblem(
                psiEl,
                SnakemakeBundle.message("INSP.NAME.redundant.coma.title"),
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                fix
        )

        fun checkElement(
                element: PsiElement
        ){
            val args = element.children
            for (arg in args){
                if (arg.elementType !== PyTokenTypes.END_OF_LINE_COMMENT
                        && arg.elementType !== PyTokenTypes.COMMA) return
                this.problemRegiser(arg)
            }
        }

        override fun visitElement(element: PsiElement) {
            if ((element.lastChild?.elementType !== PyTokenTypes.COMMA
                            && element.lastChild?.elementType !== PyTokenTypes.END_OF_LINE_COMMENT)
                    || element.parent !is SmkArgsSection) return
            else if (element.lastChild?.elementType == PyTokenTypes.COMMA) this.problemRegiser(element.lastChild)
            else if (element.lastChild?.elementType == PyTokenTypes.END_OF_LINE_COMMENT) this.checkElement(element)
//            if ((node.lastChild?.elementType !== PyTokenTypes.COMMA
//                            && node.lastChild?.elementType !== PyTokenTypes.END_OF_LINE_COMMENT)
//                            || node.parent !is SmkArgsSection) return
//                this.problemRegiser(node.lastChild)
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