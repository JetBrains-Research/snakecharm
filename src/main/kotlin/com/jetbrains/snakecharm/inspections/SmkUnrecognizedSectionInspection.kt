package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.elementType
import com.intellij.codeInspection.ui.ListEditForm
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.MODULE_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.RULE_OR_CHECKPOINT_SECTION_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SUBWORKFLOW_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.USE_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes
import com.jetbrains.snakecharm.inspections.quickfix.AddIgnoredElementQuickFix
import com.jetbrains.snakecharm.lang.SnakemakeNames.CHECKPOINT_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.MODULE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.RULE_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.SUBWORKFLOW_KEYWORD
import com.jetbrains.snakecharm.lang.SnakemakeNames.USE_KEYWORD
import javax.swing.JComponent

class SmkUnrecognizedSectionInspection : SnakemakeInspection() {
    @JvmField
    val ignoredItems: ArrayList<String> = arrayListOf()

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
            isSectionRecognized(st, SUBWORKFLOW_SECTIONS_KEYWORDS, SUBWORKFLOW_KEYWORD)
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            if (st.originalElement.elementType == SmkElementTypes.USE_ARGS_SECTION_STATEMENT) {
                isSectionRecognized(st, USE_SECTIONS_KEYWORDS, USE_KEYWORD)
            } else {
                isSectionRecognized(
                    st,
                    RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS,
                    "$RULE_KEYWORD' or '$CHECKPOINT_KEYWORD"
                )
            }
        }

        override fun visitSmkModuleArgsSection(st: SmkModuleArgsSection) {
            isSectionRecognized(st, MODULE_SECTIONS_KEYWORDS, MODULE_KEYWORD)
        }

        /**
         * Checks whether [argsSection] name identifier and section keyword are not null and
         * whether the section keyword is a member of the [setOfValidNames]. If no, it shows a weak warning.
         */
        private fun isSectionRecognized(
            argsSection: SmkArgsSection,
            setOfValidNames: Set<String>,
            sectionName: String
        ) {
            val sectionNamePsi = argsSection.nameIdentifier
            val sectionKeyword = argsSection.sectionKeyword

            if (sectionNamePsi != null && sectionKeyword != null && sectionKeyword !in setOfValidNames
                && sectionKeyword !in ignoredItems
            ) {
                val appropriateSection = getSectionBySubsection(sectionKeyword)
                val message = if (appropriateSection == null) {
                    SnakemakeBundle.message("INSP.NAME.section.unrecognized.message", sectionKeyword)
                } else {
                    SnakemakeBundle.message(
                        "INSP.NAME.section.unexpected",
                        sectionKeyword,
                        sectionName,
                        appropriateSection
                    )
                }
                registerProblem(
                    sectionNamePsi,
                    message,
                    ProblemHighlightType.WEAK_WARNING,
                    null, AddIgnoredElementQuickFix(sectionKeyword)
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

    private fun getSectionBySubsection(name: String) = when (name) {
        in RULE_OR_CHECKPOINT_SECTION_KEYWORDS -> RULE_KEYWORD
        in SUBWORKFLOW_SECTIONS_KEYWORDS -> SUBWORKFLOW_KEYWORD
        in MODULE_SECTIONS_KEYWORDS -> MODULE_KEYWORD
        in USE_SECTIONS_KEYWORDS -> USE_KEYWORD
        else -> null
    }
}

