package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.AdvancedWildcardsCollector
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkUse
import com.jetbrains.snakecharm.lang.psi.WildcardDescriptor

class SmkWildcardInNotOverriddenSectionInspection : SnakemakeInspection() {
    companion object {
        val KEY_DefiningSections = Key<HashMap<SmkRuleOrCheckpoint, Ref<List<WildcardDescriptor>>>>("SmkWildcardInNotOverriddenSectionInspection_DefiningSections_Wildcards")
        val KEY_AllSections = Key<HashMap<SmkRuleOrCheckpoint, Ref<List<WildcardDescriptor>>>>("SmkWildcardInNotOverriddenSectionInspection_AllSections _Wildcards")
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitSmkUse(use: SmkUse) {
            val cachedWildcardsByRuleInDefSection = session.putUserDataIfAbsent(KEY_DefiningSections, hashMapOf())
            val cachedWildcardsByRuleInAllSections = session.putUserDataIfAbsent(KEY_AllSections, hashMapOf())
            val availableWildcardsCollector = AdvancedWildcardsCollector(
                visitDefiningSections = true,
                visitExpandingSections = false,
                ruleLike = use,
                cachedWildcardsByRule = cachedWildcardsByRuleInDefSection
            )
            val expandingSectionsWildcardsCollector = AdvancedWildcardsCollector(
                visitAllSections = true,
                visitDefiningSections = false,
                visitExpandingSections = false,
                ruleLike = use,
                cachedWildcardsByRule = cachedWildcardsByRuleInAllSections
            )
            val sections = use.getSections().map { it.sectionKeyword }

            val availableWildcards = availableWildcardsCollector.getDefinedWildcards().map { it.text }.toSet()
            expandingSectionsWildcardsCollector.getDefinedWildcards().forEach { descriptor ->
                val section = descriptor.sectionName
                if (section != null && section !in sections && descriptor.text !in availableWildcards) {
                    // It's not overridden
                    registerProblem(
                        use.nameIdentifier,
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