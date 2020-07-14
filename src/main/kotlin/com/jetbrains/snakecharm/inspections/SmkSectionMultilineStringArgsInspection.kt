package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.*
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflowArgsSection

class SmkSectionMultilineStringArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        private val stringVisitor = object : MultilineStringVisitor() {
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
            argumentList?.arguments?.forEach { arg ->
                arg.accept(stringVisitor)
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.section.multiline.string.args")


    abstract class MultilineStringVisitor : PyElementVisitor() {
        abstract fun reportSmkProblem(psiElement: PsiElement, text: String)

        override fun visitPyBinaryExpression(node: PyBinaryExpression) {
            node.acceptChildren(this)
        }

        override fun visitPyParenthesizedExpression(node: PyParenthesizedExpression) {
            node.acceptChildren(this)
        }

        override fun visitPyStringLiteralExpression(strExpr: PyStringLiteralExpression) {
            if (strExpr.decodedFragments.size < 2) {
                // N/A
                return
            }

            val isMultilineString = strExpr.stringElements.any { x ->
                x.nextSibling is PsiWhiteSpace && x.nextSibling.textContains('\n')
            }
            if (!isMultilineString) {
                // N/A
                return
            }

            val section = PsiTreeUtil.getParentOfType(strExpr, SmkArgsSection::class.java)
            val sectionKeyword = section?.sectionKeyword

            if (sectionKeyword != null) {
                reportSmkProblem(
                    strExpr,
                    SnakemakeBundle.message("INSP.NAME.section.multiline.string.args.message", sectionKeyword)
                )
            }
        }
    }
}
