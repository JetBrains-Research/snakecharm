package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.python.inspections.quickfix.PyRemoveArgumentQuickFix
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.*

class SmkSectionRedeclarationInspection : SnakemakeInspection() {
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

        override fun visitSmkSubworkflow(subworkflow: SmkSubworkflow) {
            visitSMKRuleLike(subworkflow)
        }

        private fun visitSMKRuleLike(rule: SmkRuleLike<SmkArgsSection>) {
            val sectionNamesSet = HashSet<String>()

            rule.getSections().forEach {
                val name = it.name ?: return
                if (sectionNamesSet.contains(name)) {
                    registerProblem(
                            it,
                            SnakemakeBundle.message("INSP.NAME.section.redeclaration.message", name),
                            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                            null, RemoveSectionQuickFix()
                    )
                }
                sectionNamesSet.add(name)
            }
        }
    }

    private class RemoveSectionQuickFix : LocalQuickFix {
        override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.remove.section.family")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) { descriptor.psiElement.delete() }
        }
    }
}