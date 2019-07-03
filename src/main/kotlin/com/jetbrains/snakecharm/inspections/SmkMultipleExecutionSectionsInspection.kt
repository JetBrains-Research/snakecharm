package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement
import com.jetbrains.snakecharm.lang.psi.SMKRuleRunParameter

class SmkMultipleExecutionSectionsInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSMKRule(smkRule: SMKRule) {
            if (!SnakemakeLanguageDialect.isInsideSmkFile(smkRule)) {
                return
            }

            var executionSectionOccurred = false

            val sections = smkRule.getSections()
            for (st in sections) {
                if (st is SMKRuleParameterListStatement) {
                    val sectionName = st.section.text ?: return
                    val isExecutionSection = sectionName in SMKRuleParameterListStatement.EXECUTION_KEYWORDS

                    if (executionSectionOccurred && isExecutionSection) {
                        registerProblem(
                                st,
                                SnakemakeBundle.message("INSP.NAME.multiple.run.or.shell.section")
                        )
                    }

                    if (isExecutionSection) {
                        executionSectionOccurred = true
                    }
                } else if (st is SMKRuleRunParameter) {
                    if (executionSectionOccurred) {
                        registerProblem(
                                st,
                                SnakemakeBundle.message("INSP.NAME.multiple.run.or.shell.section")
                        )
                    }
                    executionSectionOccurred = true
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.multiple.run.or.shell.section")
}