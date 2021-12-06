package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.parentOfType
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
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

        override fun visitSmkUse(use: SmkUse) {
            val logSection = use.getSectionByName(SnakemakeNames.SECTION_LOG) ?: return
            (use.getDefinedReferencesOfImportedRuleNames()?.toList() ?: use.getImportedRules())?.forEach {
                handleUseReference(it, logSection)
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
            val name = rule.name ?: return
            val quickfix = if (shellSection != null) {
                CreateLogFileInShellSection(shellSection, name)
            } else {
                val runSection = ruleSections.firstOrNull { it is SmkRunSection } as SmkRunSection?
                when {
                    runSection != null -> CreateLogFileInRunSection(runSection, name)
                    else -> CreateShellSectionWithLogReference(
                        rule,
                        name,
                        originalLogSection.getPreviousOffset() ?: " "
                    )
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
                    quickfix
                )
            }
        }

        private fun handleUseReference(
            ruleReference: PsiElement,
            logSection: SmkRuleOrCheckpointArgsSection
        ) {
            var resolveResult: PsiElement?
            var reference = ruleReference
            // Searching for origin element, it must be a rule or checkpoint
            while (true) {
                resolveResult = if (reference is SmkReferenceExpression) reference.reference.resolve() else reference
                reference = when (resolveResult) {
                    is SmkRule, is SmkCheckPoint -> break
                    is SmkReferenceExpression -> resolveResult
                    else -> {
                        // Sometimes reference of overridden rule refer not to SmkUse
                        // But to its node, so we need to handle this cases
                        val useParent =
                            if (resolveResult is SmkUse) resolveResult else resolveResult?.parentOfType() ?: return
                        useParent.getDefinedReferencesOfImportedRuleNames()?.firstOrNull {
                            // Tries to get an appropriate reference if there are declared explicitly
                            val res = it.reference.resolve()
                            if (res is SmkUse) {
                                res.getProducedRulesNames().any { pair -> pair.first == ruleReference.text }
                            } else {
                                val name = it.name
                                name != null && ruleReference.text == useParent.name?.replace("*", name)
                            }
                        } ?: useParent.getProducedRulesNames()
                            // If rules were imported by pattern '*', tries to detect an appropriate one
                            // Using list of produced names
                            .firstOrNull { it.first == ruleReference.text && it.second != useParent.nameIdentifier }?.second
                        // If all rules from module were overridden as one, gets the last rule
                        ?: useParent.getImportedRules()?.lastOrNull() ?: return
                    }
                }
            }
            if (resolveResult is SmkRuleOrCheckpoint) {
                val ruleLogSection = resolveResult.getSectionByName(SnakemakeNames.SECTION_LOG)
                val message =
                    SnakemakeBundle.message(
                        "INSP.NAME.unused.log.section.in.special.rule",
                        resolveResult.name ?: return
                    )
                visitSMKRuleLike(resolveResult, logSection, ruleLogSection, message)
            }
        }
    }

    private class CreateLogFileInShellSection(expr: PsiElement, val sectionName: String) :
        LocalQuickFixOnPsiElement(expr) {

        override fun getFamilyName() = SnakemakeBundle.message(
            "INSP.INTN.unused.log.fix.add.to.shell.section",
            REDIRECT_STDERR_STDOUT_TO_LOG_CMD_TEXT,
            sectionName
        )

        override fun getText() = familyName

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val doc = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

            val shellSection = (startElement as SmkArgsSection)
            val stringArgument = shellSection.argumentList?.arguments?.firstOrNull {
                it is PyStringLiteralExpression
            }

            if (stringArgument == null) {
                val indent = shellSection.getPreviousOffset()?.replace("\n", "")
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

    private class CreateLogFileInRunSection(expr: SmkRunSection, val sectionName: String) :
        LocalQuickFixOnPsiElement(expr) {

        override fun getFamilyName() =
            SnakemakeBundle.message("INSP.INTN.unused.log.fix.add.to.run.section", sectionName)

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

    private class CreateShellSectionWithLogReference(expr: PsiElement, val sectionName: String, val logIndent: String) :
        LocalQuickFixOnPsiElement(expr) {

        override fun getFamilyName() =
            SnakemakeBundle.message("INSP.INTN.unused.log.fix.create.shell.section", sectionName)

        override fun getText() = familyName

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val doc = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

            val ruleLike = (startElement as SmkRuleOrCheckpoint)
            val lastArg = ruleLike.getSections().lastOrNull()
            val indent = lastArg?.getPreviousOffset() ?: logIndent
            doc.insertString(
                (lastArg ?: ruleLike).endOffset,
                "${indent}shell: \"echo TODO$REDIRECT_STDERR_STDOUT_TO_LOG_CMD_TEXT\""
            )
            PsiDocumentManager.getInstance(project).commitDocument(doc)
        }
    }
}