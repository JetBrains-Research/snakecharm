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

        private fun checkCommaInPyArgumentList(
                element: PyArgumentList
        ): PsiElement? {

            element.node.getChildren(null).reversed().forEach {
                val elType = it.elementType

                if (elType !== PyTokenTypes.END_OF_LINE_COMMENT
                        && elType !== PyTokenTypes.COMMA
                        && elType !== TokenType.WHITE_SPACE) {
                    return null
                }
                if (elType == PyTokenTypes.COMMA) {
                    return it.psi
                }
            }

            return null
        }

        override fun visitPyArgumentList(node: PyArgumentList) {
            if ((node.lastChild?.elementType !== PyTokenTypes.COMMA
                            && node.lastChild.elementType !== PyTokenTypes.END_OF_LINE_COMMENT)
                    || node.parent !is SmkArgsSection) return

            val problemEl = when (node.lastChild?.elementType) {
                PyTokenTypes.COMMA -> node.lastChild
                PyTokenTypes.END_OF_LINE_COMMENT -> checkCommaInPyArgumentList(node)
                else -> null
            }

            problemEl?.let {
                holder.registerProblem(
                        it,
                        SnakemakeBundle.message("INSP.NAME.redundant.comma.title"),
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        FIX
                )
            }
        }
    }


    private class RemoveSectionQuickFix : LocalQuickFix {
        override fun getFamilyName() = SnakemakeBundle.message("INSP.NAME.redundant.comma.fix.message")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) { descriptor.psiElement.delete() }
        }
    }

    companion object {
        private val FIX = RemoveSectionQuickFix()
    }
}