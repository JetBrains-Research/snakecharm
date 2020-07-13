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
                val visitor = SmkMultilineStringArgsInspectionVisitor()
                arg.accept(visitor)
                if (visitor.warning) {
                    registerProblem(
                            arg,
                            SnakemakeBundle.message("INSP.NAME.section.multiline.string.args.message",
                                    section.sectionKeyword!!)
                    )
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.section.multiline.string.args")
}

class SmkMultilineStringArgsInspectionVisitor : PyElementVisitor() {

    var warning: Boolean = false
        private set

    override fun visitPyBinaryExpression(node: PyBinaryExpression) {
        node.leftExpression.accept(this)
        if (!warning) {
            node.rightExpression?.accept(this)
        }
    }

    override fun visitPyParenthesizedExpression(node: PyParenthesizedExpression) {
        node.containedExpression?.accept(this)
    }

    override fun visitPyStringLiteralExpression(node: PyStringLiteralExpression) {
        if (node.decodedFragments.size > 1 && node.stringElements.any { x ->
                    x.nextSibling is PsiWhiteSpace && x.nextSibling.textContains('\n')
                }) {
            warning = true
        }
    }
}
