package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeApiService
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkUse

class SmkExecutionSubsectionInUseSectionInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {
        val api = SnakemakeApiService.getInstance(holder.project)

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            val sectionNamePsi = st.nameIdentifier
            val sectionKeyword = st.sectionKeyword
            if (st.getParentRuleOrCheckPoint() is SmkUse &&
                sectionKeyword in (api.getExecutionSectionsKeyword() + SnakemakeNames.SECTION_RUN)
            ) {
                registerProblem(sectionNamePsi, SnakemakeBundle.message("INSP.NAME.unexpected.execution.section"))
            }
        }
    }
}
