package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.HintAction
import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.problems.Problem
import com.intellij.psi.PsiFile
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
                                SwapTwoSectionsHintAction(executionSection, st)
                        )
                    }
                }
            }
        }
    }

    private class SwapTwoSectionsHintAction(
            private val firstSection: SmkRuleOrCheckpointArgsSection,
            private val secondSection: SmkRuleOrCheckpointArgsSection
    ) : HintAction {
        override fun startInWriteAction() = false

        override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.move.rule.section.upwards.family")

        override fun getText(): String = SnakemakeBundle.message("INSP.INTN.move.rule.section.upwards.text")

        override fun showHint(editor: Editor) = true

        override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?) = true

        override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
            val document = editor?.document ?: return
            val startOffset1 = firstSection.textOffset
            val endOffset1 = firstSection.textOffset + firstSection.textLength
            val startOffset2 = secondSection.textOffset
            val endOffset2 = secondSection.textOffset + firstSection.textLength
            val secondSectionIndent = startOffset2 - endOffset1 - 1
            val text1 = firstSection.text
            // TODO what happens to multiline sections on Windows, where string separators are different?
            WriteCommandAction.runWriteCommandAction(project) {
                document.replaceString(startOffset1, endOffset1, secondSection.text)
                if (text1.length > secondSection.textLength) {
                    val endOffset = if (endOffset1 < document.textLength) endOffset1 else document.textLength
                    document.replaceString(startOffset1 + secondSection.textLength, endOffset, "")
                }
                if (startOffset2 < document.textLength) {
                    document.replaceString(startOffset2, endOffset2, "")
                    document.insertString(startOffset2, text1)
                } else {
                    document.insertString(document.textLength, "\n")
                    document.insertString(document.textLength, " ".repeat(secondSectionIndent))
                    document.insertString(document.textLength, text1)
                }
            }
        }
    }
}
