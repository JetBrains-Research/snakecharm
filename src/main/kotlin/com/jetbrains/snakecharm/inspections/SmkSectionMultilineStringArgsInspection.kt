package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyParenthesizedExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeApiService
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflowArgsSection

class SmkSectionMultilineStringArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {
        val apiService = SnakemakeApiService.getInstance(holder.project)

        private val stringVisitor = object : MultilineStringVisitor() {
            override fun reportSmkProblem(psiElement: PsiElement, text: String) {
                registerProblem(psiElement, text)
            }
        }

        override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
            checkArgsSection(st)
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            checkArgsSection(st)
        }

        private fun checkArgsSection(st: SmkArgsSection) {
            val argumentList = st.argumentList
            val keyword = st.sectionKeyword
            val contextKeyword = st.getParentRuleOrCheckPoint()?.sectionKeyword
            if (argumentList == null || keyword == null || contextKeyword == null || apiService.isSubsectionSingleArgumentOnly(keyword, contextKeyword)) {
                return
            }

            argumentList.arguments.forEach { arg ->
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
            requireNotNull(sectionKeyword) { "Should be called for strings inside SmkArgsSection" }

            reportSmkProblem(
                strExpr,
                SnakemakeBundle.message("INSP.NAME.section.multiline.string.args.message", sectionKeyword)
            )
        }
    }
}
