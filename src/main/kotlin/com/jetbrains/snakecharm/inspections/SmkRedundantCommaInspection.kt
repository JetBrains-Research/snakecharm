package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.util.elementType
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection

class SmkRedundantCommaInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        private fun problemRegiser(psiEl: PsiElement?) = psiEl?.let {
            holder.registerProblem(
                    it,
                    SnakemakeBundle.message("INSP.NAME.redundant.coma.title"),
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    fix
        )
        }

        private fun checkCommaInPyArgumentList(
                element: PyArgumentList
        ): PsiElement? {

            element.node.getChildren(null).reversed().forEach {
                if (it.elementType !== PyTokenTypes.END_OF_LINE_COMMENT
                        && it.elementType !== PyTokenTypes.COMMA
                        && it.elementType !== TokenType.WHITE_SPACE) {
                    return null
                }
                if (it.elementType == PyTokenTypes.COMMA) {
                    return it.psi
                }
            }

            return null
        }

        override fun visitPyArgumentList(node: PyArgumentList) {
            if ((node.lastChild?.elementType !== PyTokenTypes.COMMA
                            && node.lastChild?.elementType !== PyTokenTypes.END_OF_LINE_COMMENT)
                    || node.parent !is SmkArgsSection) return

            var problemEl: PsiElement? = null

            if (node.lastChild?.elementType == PyTokenTypes.COMMA) {
                problemEl = node.lastChild
            } else if (node.lastChild?.elementType == PyTokenTypes.END_OF_LINE_COMMENT) {
                problemEl = checkCommaInPyArgumentList(node)
                println(problemEl)
            }

            problemRegiser(problemEl)
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