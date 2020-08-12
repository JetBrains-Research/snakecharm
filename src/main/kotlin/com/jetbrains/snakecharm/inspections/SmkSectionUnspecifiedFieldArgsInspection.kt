package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.types.SmkCheckpointType
import com.jetbrains.snakecharm.lang.psi.types.SmkRulesType

class SmkSectionUnspecifiedFieldArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        private val stringVisitor = object : MistypedStringVisitor() {
            override fun reportSmkProblem(psiElement: PsiElement, text: String) {
                registerProblem(psiElement, text)
            }
        }

        override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
            checkArgumentList(st.argumentList)
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            checkArgumentList(st.argumentList)
        }

        private fun checkArgumentList(argumentList: PyArgumentList?) {
            val args = argumentList?.arguments ?: emptyArray()
            args.forEach { arg ->
                arg.accept(stringVisitor)
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.section.unspecified.field.args")

    abstract class MistypedStringVisitor : PyElementVisitor() {
        abstract fun reportSmkProblem(psiElement: PsiElement, text: String)

        override fun visitPyBinaryExpression(node: PyBinaryExpression) {
            node.acceptChildren(this)
        }

        override fun visitPyParenthesizedExpression(node: PyParenthesizedExpression) {
            node.acceptChildren(this)
        }

        override fun visitPyReferenceExpression(node: PyReferenceExpression) {
            val childQualified = PsiTreeUtil.getChildOfType(node, PyQualifiedExpression::class.java) ?: return
            val childType = TypeEvalContext.codeAnalysis(node.project, node.containingFile).getType(childQualified)
            if (childType is SmkRulesType || childType is SmkCheckpointType) {
                reportSmkProblem(
                        node as PsiElement,
                        SnakemakeBundle.message("INSP.NAME.section.unspecified.field.args.message", node.text)
                )
            }
        }
    }
}
