package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement

class SnakemakeRuleKeywordsAfterExecutionInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSMKRule(smkRule: SMKRule) {
            if (smkRule.containingFile.language != SnakemakeLanguageDialect) {
                return
            }

            var executionSectionOccured = false
            var executionSectionName = "execution"

            val statements = smkRule.statementList.statements
            for (st in statements) {
                if (st is SMKRuleParameterListStatement) {
                    val sectionName = st.section.text ?: return
                    val isRunSection = SMKRuleParameterListStatement.EXECUTION_KEYWORDS.contains(sectionName)
                    if (isRunSection) {
                        executionSectionOccured = true
                        executionSectionName = sectionName
                    }

                    if (executionSectionOccured && !isRunSection) {
                        registerProblem(st.section,
                                SnakemakeBundle.message("INSP.NAME.rule.keywords.after.$0", executionSectionName))
                    }
                }
            }

        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.rule.keywords.after.$0", "execution")
}
