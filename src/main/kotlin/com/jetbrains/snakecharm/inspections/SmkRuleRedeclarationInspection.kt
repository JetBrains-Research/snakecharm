package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SMKRule

class SmkRuleRedeclarationInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        private val ruleNames = mutableSetOf<String>()

        override fun visitSMKRule(smkRule: SMKRule) {
            if (!SnakemakeLanguageDialect.isInsideSmkFile(smkRule)) {
                return
            }

            val ruleName = smkRule.name ?: return
            if (ruleNames.contains(ruleName)) {
                registerProblem(smkRule.getNameNode()?.psi,
                        SnakemakeBundle.message("INSP.NAME.rule.redeclaration"))
            } else {
                ruleNames.add(ruleName)
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.rule.redeclaration")
}