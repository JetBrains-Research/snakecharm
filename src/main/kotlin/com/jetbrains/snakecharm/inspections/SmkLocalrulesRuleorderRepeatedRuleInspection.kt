package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkWorkflowLocalrulesSection
import com.jetbrains.snakecharm.lang.psi.SmkWorkflowRuleorderSection

class SmkLocalrulesRuleorderRepeatedRuleInspection  : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitSmkWorkflowLocalrulesSection(st: SmkWorkflowLocalrulesSection) {
            checkArgumentRepetition(st.argumentList)
        }

        override fun visitSmkWorkflowRuleorderSection(st: SmkWorkflowRuleorderSection) {
            checkArgumentRepetition(st.argumentList)
        }

        private fun checkArgumentRepetition(argumentList: PyArgumentList?) {
            val ruleNames = mutableSetOf<String>()
            argumentList?.arguments?.forEach { expr ->
                val name = expr.name ?: return
                if (!ruleNames.add(name)) {
                    registerProblem(
                            expr,
                            SnakemakeBundle.message("INSP.NAME.localrules.ruleorder.repeated.rule"))
                }
            }
        }


    }
}