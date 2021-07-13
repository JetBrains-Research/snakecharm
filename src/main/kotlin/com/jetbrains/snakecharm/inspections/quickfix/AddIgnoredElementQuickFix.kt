package com.jetbrains.snakecharm.inspections.quickfix

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.inspections.SmkUnrecognizedSectionInspection

class AddIgnoredElementQuickFix(section: PsiElement) : LocalQuickFix, LowPriorityAction {
    private val sectionName = section.text

    override fun getName() = SnakemakeBundle.message("INSP.NAME.section.unrecognized.ignored.add", sectionName)
    override fun getFamilyName() = SnakemakeBundle.message("INSP.NAME.section.unrecognized.ignored.add", sectionName)

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val context = descriptor.psiElement

        com.intellij.codeInspection.ex.modifyAndCommitProjectProfile(project) {
            val inspection = it.getUnwrappedTool(
                SmkUnrecognizedSectionInspection::class.java.simpleName,
                context
            ) as SmkUnrecognizedSectionInspection
            if (sectionName !in inspection.ignoredItems) {
                inspection.ignoredItems.add(sectionName)
            }
        }
    }
}