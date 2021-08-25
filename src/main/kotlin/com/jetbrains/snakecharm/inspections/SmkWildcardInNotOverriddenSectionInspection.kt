package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.AdvanceWildcardsCollector
import com.jetbrains.snakecharm.lang.psi.SmkUse

class SmkWildcardInNotOverriddenSectionInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitSmkUse(use: SmkUse) {
            val availableWildcardsCollector = AdvanceWildcardsCollector(
                visitDefiningSections = true,
                visitExpandingSections = false,
                ruleLike = use,
                collectDescriptors = true,
                cachedWildcardsByRule = null
            )
            val expandingSectionsWildcardsCollector = AdvanceWildcardsCollector(
                visitAllSections = true,
                visitDefiningSections = false,
                visitExpandingSections = false,
                ruleLike = use,
                collectDescriptors = true,
                cachedWildcardsByRule = null
            )
            val sections = use.getSections().map { it.sectionKeyword }

            val availableWildcards = availableWildcardsCollector.getDefinedWildcardDescriptors().map { it.text }.toSet()
            expandingSectionsWildcardsCollector.getDefinedWildcardDescriptors().forEach { descriptor ->
                val section = descriptor.sectionName
                if (section != null && section !in sections && descriptor.text !in availableWildcards) {
                    // It's not overridden
                    registerProblem(
                        use,
                        SnakemakeBundle.message(
                            "INSP.NAME.undeclared.wildcard.in.not.overridden.section",
                            section,
                            descriptor.text
                        )
                    )
                }
            }
        }
    }
}