package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.*

class SmkRuleSectionAfterExecutionInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSMKRule(rule: SMKRule) {
            visitSMKRuleLike(rule)
        }

        override fun visitSMKCheckPoint(checkPoint: SMKCheckPoint) {
            visitSMKRuleLike(checkPoint)
        }

        private fun visitSMKRuleLike(rule: SmkRuleLike<SmkSectionStatement>) {
            var executionSectionOccurred = false
            var executionSectionName: String? = null

            val sections = rule.getSections()
            for (st in sections) {
                if (st is SMKRuleParameterListStatement) {
                    val sectionName = st.section.text ?: return
                    val isExecutionSection = sectionName in SMKRuleParameterListStatement.EXECUTION_KEYWORDS
                    if (isExecutionSection) {
                        executionSectionOccurred = true
                        executionSectionName = sectionName
                    }

                    if (executionSectionOccurred && !isExecutionSection) {
                        requireNotNull(executionSectionName)

                        registerProblem(st,
                                SnakemakeBundle.message(
                                        "INSP.NAME.rule.section.after.execution.message",
                                        sectionName,
                                        executionSectionName
                                )
                        )
                    }
                }
            }

        }
    }
}
