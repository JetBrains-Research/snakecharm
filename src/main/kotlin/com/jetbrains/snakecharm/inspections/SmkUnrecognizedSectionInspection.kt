package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SUBWORKFLOW_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflowArgsSection

class SmkUnrecognizedSectionInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
            val sectionNamePsi = st.nameIdentifier
            val sectionKeyword = st.sectionKeyword

            if (sectionNamePsi != null && sectionKeyword != null && sectionKeyword !in SUBWORKFLOW_SECTIONS_KEYWORDS) {
                registerProblem(
                    sectionNamePsi,
                    SnakemakeBundle.message("INSP.NAME.section.unrecognized.message", sectionKeyword)
                )
            }
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            val sectionNamePsi = st.nameIdentifier
            val sectionKeyword = st.sectionKeyword

            if (sectionNamePsi != null && sectionKeyword != null && sectionKeyword !in RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS) {
                registerProblem(
                    sectionNamePsi,
                    SnakemakeBundle.message("INSP.NAME.section.unrecognized.message", sectionKeyword)
                )
            }
        }

        // TODO: when any workflow section will be allowed,
        //  see https://github.com/JetBrains-Research/snakecharm/issues/334
        // override fun visitSmkWorkflowArgsSection(st: SmkWorkflowArgsSection) {
        //     super.visitSmkWorkflowArgsSection(st)
        // }
    }
}