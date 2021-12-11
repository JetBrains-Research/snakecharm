package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.util.elementType
import com.jetbrains.python.PyTokenTypes.COMMA
import com.jetbrains.python.PyTokenTypes.END_OF_LINE_COMMENT
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection

class SmkRedundantCommaInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

        private fun findRedundantComma(argList: PyArgumentList): PsiElement? {
            argList.node.getChildren(null).reversed().forEach { node ->
                val elType = node.elementType

                if (elType !== END_OF_LINE_COMMENT
                    && elType !== COMMA
                    && elType !== TokenType.WHITE_SPACE
                ) {
                    return null
                }

                if (elType == COMMA) {
                    return node.psi
                }
            }

            return null
        }

        override fun visitPyArgumentList(argList: PyArgumentList) {
            if (argList.parent !is SmkArgsSection) {
                return
            }

            val lastChild = argList.lastChild
            val redundantComma = when (lastChild?.elementType) {
                COMMA -> lastChild
                END_OF_LINE_COMMENT -> findRedundantComma(argList)
                else -> null
            }

            if (redundantComma != null) {
                holder.registerProblem(
                    redundantComma,
                    SnakemakeBundle.message("INSP.NAME.redundant.comma.title"),
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    RemoveSectionQuickFix
                )
            }
        }
    }

    object RemoveSectionQuickFix : LocalQuickFix {
        override fun getFamilyName() = SnakemakeBundle.message("INSP.NAME.redundant.comma.fix.message")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) { descriptor.psiElement.delete() }
        }
    }
}