package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiWhiteSpace
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
                if (checkRecursive(arg)) {
                    registerProblem(
                            arg,
                            SnakemakeBundle.message("INSP.NAME.section.multiline.string.args.message",
                                    section.sectionKeyword!!)
                    )
                }
            }
        }

        private fun checkRecursive(expr: PyExpression?): Boolean {
            return when (expr) {
                is PyBinaryExpression -> checkRecursive(expr.leftExpression)
                        || checkRecursive(expr.rightExpression)
                is PyParenthesizedExpression -> checkRecursive(expr.containedExpression)
                is PyStringLiteralExpression -> expr.decodedFragments.size > 1
                        && expr.stringElements.any { x ->
                    x.nextSibling is PsiWhiteSpace && x.nextSibling.textContains('\n')
                }
                else -> false
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.section.multiline.string.args")
}