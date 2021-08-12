package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes

class SmkUnexpectedSectionInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
            isSectionRecognized(st, SnakemakeAPI.SUBWORKFLOW_SECTIONS_KEYWORDS)
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            if (st.originalElement.elementType == SmkElementTypes.USE_ARGS_SECTION_STATEMENT) {
                isSectionRecognized(st, SnakemakeAPI.USE_SECTIONS_KEYWORDS)
            } else {
                isSectionRecognized(st, SnakemakeAPI.RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS)
            }
        }

        override fun visitSmkModuleArgsSection(st: SmkModuleArgsSection) {
            isSectionRecognized(st, SnakemakeAPI.MODULE_SECTIONS_KEYWORDS)
        }

        private fun isSectionRecognized(
            argsSection: SmkArgsSection,
            setOfValidNames: Set<String>
        ) {
            val sectionNamePsi = argsSection.nameIdentifier
            val sectionKeyword = argsSection.sectionKeyword
            // Not argsSection.getParentRuleOrCheckPoint() because it is widely used in the project
            val sectionName = argsSection.parentOfType<SmkRuleLike<*>>()?.sectionKeyword ?: return
            if (sectionNamePsi == null || sectionKeyword == null || sectionKeyword in setOfValidNames) {
                return
            }
            val appropriateSection = SmkUnrecognizedSectionInspection.getSectionBySubsection(sectionKeyword) ?: return
            registerProblem(
                sectionNamePsi,
                SnakemakeBundle.message(
                    "INSP.NAME.section.unexpected",
                    sectionKeyword,
                    sectionName,
                    appropriateSection
                )
            )
        }
    }
}
