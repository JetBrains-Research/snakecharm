package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SMKCheckPoint
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SmkRuleLike
import com.jetbrains.snakecharm.lang.psi.SmkSectionStatement

class SmkSectionRedeclarationInspection : SnakemakeInspection() {
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
            val sectionNamesSet = HashSet<String>()

            rule.getSections().forEach {
                val name = it.name ?: return
                if (sectionNamesSet.contains(name)) {
                    registerProblem(
                            it,
                            SnakemakeBundle.message("INSP.NAME.section.redeclaration.message", name),
                            ProblemHighlightType.LIKE_UNUSED_SYMBOL
                    )
                }
                sectionNamesSet.add(name)
            }

        }
    }
}