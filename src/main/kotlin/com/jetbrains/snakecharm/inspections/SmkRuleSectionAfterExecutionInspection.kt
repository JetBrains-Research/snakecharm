package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.*

class SmkRuleSectionAfterExecutionInspection : SnakemakeInspection() {
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

        private fun visitSMKRuleLike(rule: SmkRuleLike<SmkArgsSection>) {
            var executionSection: SmkRuleOrCheckpointArgsSection? = null

            val sections = rule.getSections()
            for (st in sections) {
                if (st is SmkRuleOrCheckpointArgsSection) {
                    val sectionName = st.sectionKeyword ?: return
                    val isExecutionSection = sectionName in SmkRuleOrCheckpointArgsSection.EXECUTION_KEYWORDS
                    if (isExecutionSection) {
                        executionSection = st
                    }

                    if (executionSection != null && !isExecutionSection) {
                        requireNotNull(executionSection.name)

                        registerProblem(st,
                                SnakemakeBundle.message(
                                        "INSP.NAME.rule.section.after.execution.message",
                                        sectionName,
                                        executionSection.name!!
                                ),
                                ProblemHighlightType.GENERIC_ERROR,
                                null,
                                MoveExecutionSectionToEndOfRuleQuickFix(SmartPointerManager.createPointer(executionSection))
                        )
                    }
                }
            }
        }
    }

    private class MoveExecutionSectionToEndOfRuleQuickFix(
            private val executionSectionPointer: SmartPsiElementPointer<SmkSection>
    ) : LocalQuickFix {
        override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.move.execution.section.down.family")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val executionSection = executionSectionPointer.element ?: return
            val executionSectionWhitespace = executionSection.prevSibling as? PsiWhiteSpace ?: return
            val containingRule = PsiTreeUtil.getParentOfType(executionSection, SmkRuleOrCheckpoint::class.java)!!
            val document = FileEditorManager.getInstance(project).selectedTextEditor?.document ?: return
            val containingRuleEndOffset =
                    containingRule.textRange.startOffset + containingRule.textLength -
                            (executionSection.textLength + executionSectionWhitespace.textLength)
            WriteCommandAction.runWriteCommandAction(project) {
                document.deleteString(
                        executionSectionWhitespace.textRange.startOffset,
                        executionSection.textRange.endOffset
                )
                document.insertString(
                        containingRuleEndOffset,
                        "${executionSectionWhitespace.text}${executionSection.text}"
                )
            }
        }
    }
}
