package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.PyElementTypes.CALL_EXPRESSION
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SNAKEMAKE_IO_METHODS_CORRECT_SECTIONS
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkMisuseUsageIOFlagMethodsInspection: SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        private fun getAllowedSectionNames(ruleOrCheckpointSectionName: String): List<String>? {
            return SNAKEMAKE_IO_METHODS_CORRECT_SECTIONS[ruleOrCheckpointSectionName]
        }

        private fun checkMethod(callReferenceExpression: PyReferenceExpression, ruleOrCheckpointSection: SmkRuleOrCheckpointArgsSection): PsiElement? {
            val allowedSectionNames = getAllowedSectionNames(callReferenceExpression.name!!) ?: return null

            if (!allowedSectionNames.contains(ruleOrCheckpointSection.name)) {
                return callReferenceExpression
            }
            return null
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {

            if (st.containingFile !is SmkFile) {
                return
            }

            val argList = st.argumentList ?: return

            argList.node.getChildren(callExpressionToken)
                    .forEach { node ->

                        val callReferenceExpression = PsiTreeUtil.getChildOfType(node.psi, PyReferenceExpressionImpl::class.java) ?: return

                        val method = checkMethod(callReferenceExpression, st)

                        val message = SnakemakeBundle.message("INSP.NAME.misuse.usage.io.flag.methods.warning.message",
                                callReferenceExpression.name!!, st.name!!)

                        if (method != null) {
                            holder.registerProblem(
                                    node.psi,
                                    message
                            )
                        }
                    }
        }
    }

    companion object {
        val callExpressionToken = TokenSet.create(CALL_EXPRESSION)
    }
}