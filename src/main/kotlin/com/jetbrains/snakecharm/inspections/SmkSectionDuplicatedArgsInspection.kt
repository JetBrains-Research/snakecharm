package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SINGLE_ARGUMENT_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS
import com.jetbrains.snakecharm.inspections.quickfix.RenameElementWithoutUsagesQuickFix
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkSectionDuplicatedArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            if (st.sectionKeyword in (RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS
                            - SINGLE_ARGUMENT_SECTIONS_KEYWORDS)) {
                checkArgumentList(st.argumentList, st)
            }
        }

        private fun checkArgumentList(
                argumentList: PyArgumentList?,
                section: SmkRuleOrCheckpointArgsSection
        ) {

            val args = argumentList?.arguments ?: emptyArray()
            args.forEachIndexed { i, arg ->
                if (args.indexOfFirst{iterator -> iterator.text == arg.text} != i) {

                    val sectionElement = arg.originalElement
                    val fixes =
                            arrayOf(
                                    RemoveArgumentQuickFix(),
                                    if (sectionElement != null) {
                                        RenameElementWithoutUsagesQuickFix(
                                                section,
                                                sectionElement.textRangeInParent.startOffset,
                                                sectionElement.textRangeInParent.endOffset
                                        )
                                    } else {
                                        null
                                    }
                            )
                    registerProblem(
                            arg,
                            SnakemakeBundle.message("INSP.NAME.section.duplicated.args.message",
                                    section.sectionKeyword!!),
                            ProblemHighlightType.WARNING,
                            null,
                            *fixes
                    )
                }
            }
        }
    }

    private class RemoveArgumentQuickFix : LocalQuickFix {
        override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.remove.duplicated.arg")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) { descriptor.psiElement.delete() }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.section.duplicated.args")
}
