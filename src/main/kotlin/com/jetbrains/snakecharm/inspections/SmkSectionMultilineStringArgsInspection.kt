package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
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

        override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
            checkArgumentList(st.argumentList)
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            checkArgumentList(st.argumentList)
        }

        private val instanceArgumentVisitor: SmkMultilineStringArgsInspectionVisitor? = null
        val myArgumentVisitor: SmkMultilineStringArgsInspectionVisitor
            get() = instanceArgumentVisitor ?: SmkMultilineStringArgsInspectionVisitor { arg, name ->
                registerProblem(arg,
                        SnakemakeBundle.message("INSP.NAME.section.multiline.string.args.message",
                                name))
            }

        private fun checkArgumentList(
                argumentList: PyArgumentList?
        ) {
            val args = argumentList?.arguments ?: emptyArray()
            args.forEach { arg ->
                arg.accept(myArgumentVisitor)
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.section.multiline.string.args")
}

class SmkMultilineStringArgsInspectionVisitor(val warnAction: (PyExpression, String) -> Unit) : PyElementVisitor() {
    override fun visitPyBinaryExpression(node: PyBinaryExpression) {
        node.acceptChildren(this)
    }

    override fun visitPyParenthesizedExpression(node: PyParenthesizedExpression) {
        node.acceptChildren(this)
    }

    override fun visitPyStringLiteralExpression(node: PyStringLiteralExpression) {
        if (node.decodedFragments.size > 1 && node.stringElements.any { x ->
                    x.nextSibling is PsiWhiteSpace && x.nextSibling.textContains('\n')
                }) {
            val section = PsiTreeUtil.getParentOfType(node, SmkArgsSection::class.java)
            warnAction(node, section?.sectionKeyword!!)
        }
    }
}
