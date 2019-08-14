package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.rename.RenameDialog
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.usageView.UsageInfo
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkCheckPoint
import com.jetbrains.snakecharm.lang.psi.SmkRule
import com.jetbrains.snakecharm.lang.psi.SmkRuleLike
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkRuleRedeclarationInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        private val ruleNames = mutableSetOf<String>()

        override fun visitSmkRule(rule: SmkRule) {
            visitSMKRuleLike(rule)
        }

        override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
            visitSMKRuleLike(checkPoint)
        }

        private fun visitSMKRuleLike(rule: SmkRuleLike<SmkRuleOrCheckpointArgsSection>) {
            val ruleName = rule.name ?: return
            if (ruleNames.contains(ruleName)) {
                val problemElement = rule.nameIdentifier ?: return
                registerProblem(
                        problemElement,
                        SnakemakeBundle.message("INSP.NAME.rule.redeclaration"),
                        ProblemHighlightType.GENERIC_ERROR,
                        null,
                        RenameRuleFix(rule)
                )
            } else {
                ruleNames.add(ruleName)
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.rule.redeclaration")

    private class RenameRuleFix(ruleOrCheckpoint: SmkRuleLike<SmkRuleOrCheckpointArgsSection>) :
            LocalQuickFixAndIntentionActionOnPsiElement(ruleOrCheckpoint) {
        private val defaultName = "DEFAULT_SNAKEMAKE_RULE_NAME"

        override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.rename.rule")

        override fun getText() = SnakemakeBundle.message("INSP.INTN.rename.rule")

        override fun startInWriteAction(): Boolean = false

        override fun invoke(
                project: Project,
                file: PsiFile,
                editor: Editor?,
                startElement: PsiElement,
                endElement: PsiElement
        ) {
            val dialog = object : RenameDialog(
                    project,
                    startElement,
                    null,
                    editor
            ) {
                override fun performRename(newName: String) {
                    val processor = object : RenameProcessor(
                            project,
                            startElement,
                            newName,
                            isSearchInComments,
                            isToSearchForTextOccurrencesForRename
                    ) {
                        override fun performRefactoring(usages: Array<out UsageInfo>) {
                            WriteCommandAction.runWriteCommandAction(project) {
                                (psiElement as PsiNamedElement).setName(newName)
                            }
                        }
                    }
                    invokeRefactoring(processor)
                }
            }

            if (ApplicationManager.getApplication().isUnitTestMode) {
                try {
                    dialog.performRename(defaultName)
                } finally {
                    dialog.close(DialogWrapper.CANCEL_EXIT_CODE) // to avoid dialog leak
                }
            } else {
                dialog.show()
            }
        }
    }
}