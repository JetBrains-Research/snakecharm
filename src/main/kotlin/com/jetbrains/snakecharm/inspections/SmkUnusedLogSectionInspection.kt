package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.util.parentOfType
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.*

class SmkUnusedLogSectionInspection : SnakemakeInspection() {
    companion object {
        val KEY = Key<HashSet<SmkRuleOrCheckpoint>>("SmkLogUnusedSection_Rules")
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
            val logSection = rule.getSections().firstOrNull {
                it.sectionKeyword == SnakemakeNames.SECTION_LOG
            } ?: return
            val sectionsByRule = session.putUserDataIfAbsent(KEY, hashSetOf())

            if (rule in sectionsByRule) {
                return
            }

            val collector = SmkSLSectionReferencesCollector(SnakemakeNames.SECTION_LOG) {
                !sectionsByRule.contains(rule)
            }
            rule.containingFile.accept(collector)
            collector.getSections()
                .forEach {
                    val parent = it.parentOfType<SmkRuleOrCheckpoint>()
                    if (parent != null && !sectionsByRule.contains(parent)) {
                        sectionsByRule.add(parent)
                    }
                }

            if (rule !in sectionsByRule) {
                registerProblem(
                    logSection,
                    SnakemakeBundle.message("INSP.NAME.unused.section"),
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                    null,
                    RemoveSectionQuickFix
                )
            }
        }
    }

    private object RemoveSectionQuickFix : LocalQuickFix {
        override fun getFamilyName() = SnakemakeBundle.message("INSP.NAME.remove.unused.section")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            WriteCommandAction.runWriteCommandAction(project) { descriptor.psiElement.delete() }
        }
    }
}