package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.EXECUTION_SECTIONS_THAT_ACCEPTS_SNAKEMAKE_PARAMS_OBJ_FROM_RULE
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.*

class SmkUnusedLogFileInspection : SnakemakeInspection() {
    companion object {
        private const val REDIRECT_STDERR_STDOUT_TO_LOG_CMD_TEXT = " >{log} 2>&1"
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitSmkUse(use: SmkUse) {
            val logSection = use.getSectionByName(SnakemakeNames.SECTION_LOG) ?: return
            use.getOverriddenRuleReferences()?.forEach {
                var resolveResult: PsiElement?
                var reference = it
                while (true) {
                    resolveResult = reference.reference.resolve()
                    when (resolveResult) {
                        is SmkRule -> break
                        is SmkReferenceExpression -> reference = resolveResult
                        else -> {
                            return@forEach
                        }
                    }
                }
                if (resolveResult != null) {
                    resolveResult as SmkRule
                    val ruleLogSection = resolveResult.getSectionByName(SnakemakeNames.SECTION_LOG)
                    val message =
                        SnakemakeBundle.message(
                            "INSP.NAME.unused.log.section.in.special.rule",
                            resolveResult.name ?: return@forEach
                        )
                    visitSMKRuleLike(resolveResult, logSection, ruleLogSection, message)
                }
            }
        }

        override fun visitSmkRule(rule: SmkRule) {
            ruleOrCheckpointHandler(rule)
        }

        override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
            ruleOrCheckpointHandler(checkPoint)
        }

        private fun ruleOrCheckpointHandler(ruleOrCheckpoint: SmkRuleOrCheckpoint) {
            val logSection = ruleOrCheckpoint.getSectionByName(SnakemakeNames.SECTION_LOG) ?: return
            val message = SnakemakeBundle.message("INSP.NAME.unused.log.section")
            visitSMKRuleLike(ruleOrCheckpoint, logSection, logSection, message)
        }

        private fun visitSMKRuleLike(
            rule: SmkRuleLike<SmkArgsSection>,
            originalLogSection: SmkArgsSection,
            logSectionWhichMustBeResolveResult: SmkArgsSection?,
            message: String
        ) {
            val ruleSections = rule.getSections()
            val skipValidation = ruleSections.any { section ->
                section.sectionKeyword in EXECUTION_SECTIONS_THAT_ACCEPTS_SNAKEMAKE_PARAMS_OBJ_FROM_RULE
            }
            if (skipValidation) {
                return
            }

            val shellSection = rule.getSectionByName(SnakemakeNames.SECTION_SHELL)
            val quickfix = if (shellSection != null) {
                CreateLogFileInShellSection(shellSection)
            } else {
                val runSection = ruleSections.firstOrNull { it is SmkRunSection } as SmkRunSection?
                when {
                    runSection != null -> CreateLogFileInRunSection(runSection)
                    else -> CreateShellSectionWithLogReference(rule)
                }
            }


            val collector = SmkSLReferencesTargetLookupVisitor(logSectionWhichMustBeResolveResult)
                .also {
                    rule.accept(it)
                }

            if (!collector.hasReferenceToTarget) {
                registerProblem(
                    originalLogSection,
                    message,
                    ProblemHighlightType.WEAK_WARNING,
                    null,
                    quickfix
                )
            }
        }
    }

    private class CreateLogFileInShellSection(expr: PsiElement) : LocalQuickFixOnPsiElement(expr) {

        override fun getFamilyName() = SnakemakeBundle.message(
            "INSP.INTN.unused.log.fix.add.to.shell.section",
            REDIRECT_STDERR_STDOUT_TO_LOG_CMD_TEXT
        )

        override fun getText() = familyName

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val doc = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

            val shellSection = (startElement as SmkArgsSection)
            val stringArgument = shellSection.argumentList?.arguments?.firstOrNull {
                it is PyStringLiteralExpression
            }

            if (stringArgument == null) {
                val indent = shellSection.prevSibling.text.replace("\n", "")
                doc.insertString(
                    shellSection.lastChild.endOffset,
                    "\n$indent$indent\"$REDIRECT_STDERR_STDOUT_TO_LOG_CMD_TEXT\""
                )
                PsiDocumentManager.getInstance(project).commitDocument(doc)
                return
            }

            doc.insertString(stringArgument.endOffset - 1, REDIRECT_STDERR_STDOUT_TO_LOG_CMD_TEXT)
            PsiDocumentManager.getInstance(project).commitDocument(doc)
        }
    }

    private class CreateLogFileInRunSection(expr: SmkRunSection) : LocalQuickFixOnPsiElement(expr) {

        override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.unused.log.fix.add.to.run.section")

        override fun getText() = familyName

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val doc = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
            // Does not support complex cases
            val runSection = (startElement as SmkRunSection)
            val lastArg = runSection.lastChild
            val indent = lastArg.prevSibling.text
            doc.insertString(lastArg.endOffset, "${indent}shell(\"echo TODO$REDIRECT_STDERR_STDOUT_TO_LOG_CMD_TEXT\")")
            PsiDocumentManager.getInstance(project).commitDocument(doc)
        }
    }

    private class CreateShellSectionWithLogReference(expr: PsiElement) : LocalQuickFixOnPsiElement(expr) {

        override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.unused.log.fix.create.shell.section")

        override fun getText() = familyName

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val doc = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

            val ruleLike = (startElement as SmkRuleOrCheckpoint)
            val lastArg = ruleLike.getSections().last()
            val indent = lastArg.prevSibling.text
            doc.insertString(lastArg.endOffset, "${indent}shell: \"echo TODO$REDIRECT_STDERR_STDOUT_TO_LOG_CMD_TEXT\"")
            PsiDocumentManager.getInstance(project).commitDocument(doc)
        }
    }
}