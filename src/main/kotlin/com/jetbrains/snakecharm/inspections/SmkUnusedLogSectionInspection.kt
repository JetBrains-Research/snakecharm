package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.endOffset
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.*

class SmkUnusedLogSectionInspection : SnakemakeInspection() {
    companion object {
        private const val FIX = " >{log} 2>&1"
    }

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
            val logSection = rule.getSectionByName(SnakemakeNames.SECTION_LOG) ?: return
            val shellSection = rule.getSectionByName(SnakemakeNames.SECTION_SHELL)
            val runSection = rule.getSections().find {
                it.sectionKeyword == SnakemakeNames.SECTION_RUN
            }
            val quickfix = when ((shellSection != null) to (runSection != null)) {
                (true to true), (true to false) -> CreateLogFileInShellSection(shellSection ?: return)
                (false to true) -> CreateLogFileInRunSection(runSection ?: return)
                else -> CreateShellSectionWithLogReference(rule)
            }

            val collector = SmkSLSectionReferencesCollector(logSection)
                .also {
                    rule.accept(it)
                }

            val skipValidation = rule.getSectionByName(SnakemakeNames.SECTION_WRAPPER) != null
                    || rule.getSectionByName(SnakemakeNames.SECTION_NOTEBOOK) != null
                    || rule.getSectionByName(SnakemakeNames.SECTION_SCRIPT) != null
                    || rule.getSectionByName(SnakemakeNames.SECTION_CWL) != null
            if (!skipValidation && !collector.hasReferenceToElement()) {
                registerProblem(
                    logSection,
                    SnakemakeBundle.message("INSP.NAME.unused.section"),
                    ProblemHighlightType.WEAK_WARNING,
                    null,
                    quickfix
                )
            }
        }
    }

    private class CreateLogFileInShellSection(expr: PsiElement) : LocalQuickFixOnPsiElement(expr) {

        override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.add.to.shell.section")

        override fun getText() = familyName

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val doc = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

            val shellSection = (startElement as SmkArgsSection)
            val stringArgument = shellSection.argumentList?.arguments?.firstOrNull {
                it is PyStringLiteralExpression
            }

            if (stringArgument == null) {
                val indent = shellSection.prevSibling.text.replace("\n", "")
                doc.insertString(shellSection.lastChild.endOffset, "\n$indent$indent\"$FIX\"")
                PsiDocumentManager.getInstance(project).commitDocument(doc)
                return
            }

            doc.insertString(stringArgument.endOffset - 1, FIX)
            PsiDocumentManager.getInstance(project).commitDocument(doc)
        }
    }

    private class CreateLogFileInRunSection(expr: PsiElement) : LocalQuickFixOnPsiElement(expr) {

        override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.add.to.run.section")

        override fun getText() = familyName

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val doc = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
            // Does not support complex cases
            val runSection = (startElement as SmkRunSection)
            val lastArg = runSection.lastChild
            val indent = lastArg.prevSibling.text
            doc.insertString(lastArg.endOffset, "${indent}shell(\"$FIX\")")
            PsiDocumentManager.getInstance(project).commitDocument(doc)
        }
    }

    private class CreateShellSectionWithLogReference(expr: PsiElement) : LocalQuickFixOnPsiElement(expr) {

        override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.create.shell.section")

        override fun getText() = familyName

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val doc = PsiDocumentManager.getInstance(project).getDocument(file) ?: return

            val ruleLike = (startElement as SmkRuleOrCheckpoint)
            val lastArg = ruleLike.getSections().last()
            val indent = lastArg.prevSibling.text
            doc.insertString(lastArg.endOffset, "${indent}shell: \"$FIX\"")
            PsiDocumentManager.getInstance(project).commitDocument(doc)
        }
    }
}