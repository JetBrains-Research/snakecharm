package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkCondaSectionNotAllowedWithRun : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            if (st.sectionKeyword == SnakemakeNames.SECTION_CONDA) {
                val ruleOrCheckPoint = st.getParentRuleOrCheckPoint()
                val runSection= ruleOrCheckPoint.getSections().find { it.sectionKeyword == SnakemakeNames.SECTION_RUN }
                if (runSection != null) {
                    registerProblem(st, SnakemakeBundle.message("INSP.NAME.conda.section.not.allowed.with.run"))
                }
            }
        }
    }
}