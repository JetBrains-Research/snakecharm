package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SMKRuleReference
import com.jetbrains.snakecharm.lang.psi.SMKWorkflowLocalRulesStatement
import com.jetbrains.snakecharm.lang.psi.SMKWorkflowRuleOrderStatement
import com.jetbrains.snakecharm.lang.psi.SmkReferenceExpression

class SmkLocalrulesRuleorderRepeatedRuleInspection  : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        private val ruleNames = mutableSetOf<String>()

        override fun visitSMKWorkflowLocalRulesStatement(st: SMKWorkflowLocalRulesStatement) {
            checkArgumentRepetition(st.ruleReferences ?: return)
        }

        override fun visitSMKWorkflowRuleOrderStatement(st: SMKWorkflowRuleOrderStatement) {
            checkArgumentRepetition(st.ruleReferences ?: return)
        }

        private fun checkArgumentRepetition(ruleReferences: List<SmkReferenceExpression>) {
            ruleReferences.forEach {
                val name = it.name ?: return
                if (!ruleNames.add(name)) {
                    registerProblem(it,
                            SnakemakeBundle.message("INSP.NAME.localrules.ruleorder.repeated.rule"),
                            ProblemHighlightType.WEAK_WARNING)
                }
            }
        }


    }
}