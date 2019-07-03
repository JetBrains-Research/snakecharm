package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SMKCheckPoint
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SmkRuleLike

class SmkRuleRedeclarationInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        private val ruleNames = mutableSetOf<String>()

        override fun visitSMKRule(rule: SMKRule) {
            visitSMKRuleLike(rule)
        }

        override fun visitSMKCheckPoint(checkPoint: SMKCheckPoint) {
            visitSMKRuleLike(checkPoint)
        }

        private fun visitSMKRuleLike(rule: SmkRuleLike) {
            val ruleName = rule.name ?: return
            if (ruleNames.contains(ruleName)) {
                registerProblem(rule.getNameNode()?.psi,
                        SnakemakeBundle.message("INSP.NAME.rule.redeclaration"))
            } else {
                ruleNames.add(ruleName)
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.rule.redeclaration")
}