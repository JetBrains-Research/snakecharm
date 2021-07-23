package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.util.elementType
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkMultilineFunctionCallInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            val args = st.children.firstOrNull { it is PyArgumentList } ?: return

            if (multilineSectionDefinition(args as PyArgumentList)) {
                return
            }

            val problemCalls = arrayListOf<PsiElement>()
            args.children.filter { it.elementType == PyElementTypes.CALL_EXPRESSION }.forEach {
                multilineFunctionCall(it.node, problemCalls)
            }

            problemCalls.forEach {
                registerProblem(
                    it,
                    SnakemakeBundle.message("INSP.NAME.multiline.func.call"),
                    RemoveSectionQuickFix
                )
            }
        }
    }

    /**
     * Checks if [arguments] starts from new line
     */
    private fun multilineSectionDefinition(arguments: PyArgumentList): Boolean {
        val colons = arguments.node.findChildByType(PyTokenTypes.COLON) ?: return false
        val whitespace = colons.treeNext ?: return false
        return whitespace.elementType == TokenType.WHITE_SPACE && whitespace.text.contains("\n")
    }

    /**
     * Checks whether there are any whitespaces in [callNode] argument list,
     * if so, checks if it contains new line character,
     * if so, adds whitespace nodes to [incorrectElements]
     */
    private fun multilineFunctionCall(callNode: ASTNode, incorrectElements: ArrayList<PsiElement>) {
        var element = callNode.findChildByType(PyElementTypes.ARGUMENT_LIST)?.firstChildNode
        while (element != null) {
            if (element.elementType == TokenType.WHITE_SPACE && element.text.contains("\n")) {
                incorrectElements.add(element.psi)
                break
            }
            element = element.treeNext
        }
    }

    private object RemoveSectionQuickFix : LocalQuickFix {
        override fun getFamilyName() = SnakemakeBundle.message("INSP.NAME.multiline.func.call.fix")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) { descriptor.psiElement.delete() }
        }
    }
}