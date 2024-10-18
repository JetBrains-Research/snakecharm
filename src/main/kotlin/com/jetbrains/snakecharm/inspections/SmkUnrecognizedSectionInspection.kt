package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.ListEditForm
import com.intellij.psi.util.elementType
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeApi.SUBWORKFLOW_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeApiService
import com.jetbrains.snakecharm.inspections.quickfix.AddIgnoredElementQuickFix
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_RUN
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes
import javax.swing.JComponent

class SmkUnrecognizedSectionInspection : SnakemakeInspection() {
    @JvmField
    val ignoredItems: ArrayList<String> = arrayListOf()

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {
        val api = SnakemakeApiService.getInstance(holder.project)

        override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
            isSectionRecognized(st, SUBWORKFLOW_SECTIONS_KEYWORDS)
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            if (st.originalElement.elementType == SmkElementTypes.USE_ARGS_SECTION_STATEMENT) {
                isSectionRecognized(st, api.getUseSectionKeywords())
            } else {
                isSectionRecognized(st, api.getRuleOrCheckpointArgsSectionKeywords())
            }
        }

        override fun visitSmkModuleArgsSection(st: SmkModuleArgsSection) {
            isSectionRecognized(st, api.getModuleSectionKeywords())
        }

        /**
         * Checks whether [argsSection] name identifier and section keyword are not null and
         * whether the section keyword is a member of the [setOfValidNames]. If no, it shows a weak warning.
         */
        private fun isSectionRecognized(
            argsSection: SmkArgsSection,
            setOfValidNames: Set<String>,
        ) {
            val sectionNamePsi = argsSection.nameIdentifier
            val sectionKeyword = argsSection.sectionKeyword

            if (sectionNamePsi != null && sectionKeyword != null && sectionKeyword !in setOfValidNames
                && sectionKeyword !in ignoredItems && !(argsSection.getParentRuleOrCheckPoint() is SmkUse
                        && sectionKeyword in (api.getExecutionSectionsKeyword() + SECTION_RUN))
            ) {
                registerProblem(
                    sectionNamePsi,
                    SnakemakeBundle.message("INSP.NAME.section.unrecognized.message", sectionKeyword),
                    AddIgnoredElementQuickFix(sectionKeyword)
                )
            }
        }

        // TODO: when any workflow section will be allowed,
        //  see https://github.com/JetBrains-Research/snakecharm/issues/334
        // override fun visitSmkWorkflowArgsSection(st: SmkWorkflowArgsSection) {
        //     super.visitSmkWorkflowArgsSection(st)
        // }
    }

    override fun createOptionsPanel(): JComponent? =
        ListEditForm(SnakemakeBundle.message("INSP.NAME.section.unrecognized.ignored"), ignoredItems).contentPanel
}

