package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.*

class SmkExecutionSubsectionInUseSectionInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            val sectionNamePsi = st.nameIdentifier
            val sectionKeyword = st.sectionKeyword
            if (st.getParentRuleOrCheckPoint() is SmkUse &&
                sectionKeyword in (SnakemakeAPI.EXECUTION_SECTIONS_KEYWORDS + SnakemakeNames.SECTION_RUN)
            ) {
                registerProblem(sectionNamePsi, SnakemakeBundle.message("INSP.NAME.unexpected.execution.section"))
            }
        }
    }
}
