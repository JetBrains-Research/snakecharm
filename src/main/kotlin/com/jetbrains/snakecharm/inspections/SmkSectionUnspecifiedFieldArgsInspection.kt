package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.*
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflowArgsSection

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
            checkArgumentList(st.argumentList, st)
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            checkArgumentList(st.argumentList, st)
        }

        private fun checkArgumentList(
                argumentList: PyArgumentList?,
                section: SmkArgsSection
        ) {
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

        override fun visitPyReferenceExpression(node: PyReferenceExpression?) {
            if (node?.firstChild?.textMatches("rules") == true) {

                reportSmkProblem(
                        node as PsiElement,
                        SnakemakeBundle.message("INSP.NAME.section.unspecified.field.args.message", node.text)
                )
            }
        }
    }
}
