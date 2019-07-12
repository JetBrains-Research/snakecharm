package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.*

class SmkSectionRedeclarationInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSmkRule(rule: SmkRule) {
            visitSMKRuleLike(rule)
        }

        override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
            visitSMKRuleLike(checkPoint)
        }

        override fun visitSmkSubworkflow(subworkflow: SmkSubworkflow) {
            visitSMKRuleLike(subworkflow)
        }

        private fun visitSMKRuleLike(rule: SmkRuleLike<SmkArgsSection>) {
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