package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType.GENERIC_ERROR
import com.intellij.codeInspection.ProblemHighlightType.WEAK_WARNING
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.inspections.quickfix.RenameElementWithoutUsagesQuickFix
import com.jetbrains.snakecharm.lang.psi.*

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

            val isTopLevelDeclaration = PsiTreeUtil.getParentOfType(
                    rule, PyStatement::class.java, true, SmkFile::class.java
            ) == null

            val (msg, severity) = when {
                isTopLevelDeclaration -> SnakemakeBundle.message("INSP.NAME.rule.redeclaration") to GENERIC_ERROR
                else -> SnakemakeBundle.message("INSP.NAME.rule.redeclaration.possible") to WEAK_WARNING
            }

            if (ruleNames.contains(ruleName)) {
                val problemElement = rule.nameIdentifier ?: return
                registerProblem(problemElement, msg, severity, null,
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