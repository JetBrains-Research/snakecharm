package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.inspections.quickfix.RenameElementWithoutUsagesQuickFix
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint
import com.jetbrains.snakecharm.lang.psi.SmkRule
import com.jetbrains.snakecharm.lang.psi.SmkRuleLike
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkRuleRedeclarationInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        private val ruleNames = mutableSetOf<String>()

        override fun visitSmkRule(rule: SmkRule) {
            visitSMKRuleLike(rule)
        }

        override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
            visitSMKRuleLike(checkPoint)
        }

        private fun visitSMKRuleLike(rule: SmkRuleLike<SmkRuleOrCheckpointArgsSection>) {
            val ruleName = rule.name ?: return
            if (ruleNames.contains(ruleName)) {
                val problemElement = rule.nameIdentifier ?: return
                registerProblem(
                        problemElement,
                        SnakemakeBundle.message("INSP.NAME.rule.redeclaration"),
                        ProblemHighlightType.GENERIC_ERROR,
                        null,
                        RenameElementWithoutUsagesQuickFix(
                                rule,
                                problemElement.textRangeInParent.startOffset,
                                problemElement.textRangeInParent.endOffset
                        )
                )
            } else {
                ruleNames.add(ruleName)
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.rule.redeclaration")
}