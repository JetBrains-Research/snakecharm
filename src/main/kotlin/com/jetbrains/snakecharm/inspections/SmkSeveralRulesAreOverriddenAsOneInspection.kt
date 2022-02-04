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
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkUse

class SmkSeveralRulesAreOverriddenAsOneInspection : SnakemakeInspection() {
    companion object {
        private const val NAME_PATTERN = "_*"
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

        override fun visitSmkUse(use: SmkUse) {
            if (use.getNewNamePattern()?.isWildcard() ?: true) {
                // There are pattern in name
                return
            }

            if (use.getImportedNamesList() == null) {
                //  'use' section doesn't change names
                return
            }
            val namePsi = use.nameIdentifier ?: return

            val overridden = use.getDefinedReferencesOfImportedRuleNames()

            if (overridden != null && overridden.size == 1) {
                // There are only one rule reference
                return
            }

            // There are '*' symbol instead of list of rules, or several rules were overridden
            if (overridden != null && overridden.size > 1) {
                // Leads to runtime exception
                registerProblem(
                    namePsi,
                    SnakemakeBundle.message("INSP.NAME.only.last.rule.will.be.overridden.list.case"),
                    AppendPatternQuickFix(namePsi)
                )
            } else {
                // Just unexpected behaviour
                registerProblem(
                    namePsi,
                    SnakemakeBundle.message("INSP.NAME.only.last.rule.will.be.overridden.pattern.case"),
                    ProblemHighlightType.WEAK_WARNING,
                    null,
                    AppendPatternQuickFix(namePsi)
                )
            }
        }
    }

    private class AppendPatternQuickFix(expr: PsiElement) : LocalQuickFixOnPsiElement(expr) {
        override fun getFamilyName(): String =
            SnakemakeBundle.message("INSP.NAME.only.last.rule.will.be.overridden.fix")

        override fun getText(): String = familyName

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val doc = PsiDocumentManager.getInstance(project).getDocument(file)
            val endOffset = endElement.endOffset
            doc!!.insertString(endOffset, NAME_PATTERN)
            PsiDocumentManager.getInstance(project).commitDocument(doc)
        }
    }
}