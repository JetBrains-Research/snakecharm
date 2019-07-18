package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.problems.Problem
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.jetbrains.python.psi.PyUtil
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
                                ProblemHighlightType.ERROR,
                                null,
                                SwapTwoSectionsQuickFix(SmartPointerManager.createPointer(executionSection))
                        )
                    }
                }
            }
        }
    }

    private class SwapTwoSectionsQuickFix(private val precedingSectionPointer: SmartPsiElementPointer<SmkSection>) : LocalQuickFix {
        override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.move.rule.section.upwards.family")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val firstSection = precedingSectionPointer.element ?: return
            val secondSection = descriptor.psiElement
            val document = FileEditorManager.getInstance(project).selectedTextEditor?.document ?: return
            val startOffset1 = firstSection.textOffset
            val endOffset1 = firstSection.textRange.endOffset
            val initialStartOffset2 = secondSection.textOffset
            val initialEndOffset2 = secondSection.textRange.endOffset
            val text1 = firstSection.text
            val text2 = secondSection.text
            WriteCommandAction.runWriteCommandAction(project) {
                document.replaceString(startOffset1, endOffset1, text2)
                if (text1.length > text2.length) {
                    val endOffset = startOffset1 + text2.length
                    document.replaceString(startOffset1 + text2.length, endOffset, "")
                }
                val startOffset2 = initialStartOffset2 + (text2.length - text1.length)
                val endOffset2 = initialEndOffset2 + (text2.length - text1.length)
                document.replaceString(startOffset2, endOffset2, text1)
            }
        }
    }
}
