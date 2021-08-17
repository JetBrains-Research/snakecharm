package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.inspections.quickfix.RenameElementWithoutUsagesQuickFix
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

        override fun visitSmkModule(module: SmkModule) {
            visitSMKRuleLike(module)
        }

        override fun visitSmkUse(use: SmkUse) {
            visitSMKRuleLike(use)
        }

        private fun visitSMKRuleLike(rule: SmkRuleLike<SmkArgsSection>) {
            val sectionNamesSet = HashSet<String>()

            rule.getSections().forEach { section ->
                val name = section.name ?: return

                if (sectionNamesSet.contains(name)) {
                    val fixes: ArrayList<LocalQuickFix> = arrayListOf(
                        RemoveSectionQuickFix
                    )
                    section.getSectionKeywordNode()?.psi?.let { sectionElement ->
                        fixes.add(
                            RenameElementWithoutUsagesQuickFix(
                                section,
                                sectionElement.textRangeInParent.startOffset,
                                sectionElement.textRangeInParent.endOffset
                            )
                        )
                    }

                    registerProblem(
                        section,
                        SnakemakeBundle.message("INSP.NAME.section.redeclaration.message", name),
                        // No suitable severity, so is WEAK WARNING in plugin.xml
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        null,
                        *fixes.toTypedArray()
                    )
                }
                sectionNamesSet.add(name)
            }
        }
    }

    private object RemoveSectionQuickFix : LocalQuickFix {
        override fun getFamilyName() = SnakemakeBundle.message("INSP.INTN.remove.section.family")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) { descriptor.psiElement.delete() }
        }
    }
}