package com.jetbrains.snakecharm.inspections.smksl

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.EXECUTION_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.inspections.SnakemakeInspection
import com.jetbrains.snakecharm.lang.SnakemakeNames.SECTION_RUN
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkWildcardsCollector
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpressionImpl

class SmkLogUnusedSection : SnakemakeInspection() {
    companion object {
        //val KEY = Key<HashMap<SmkRuleOrCheckpoint, Ref<List<String>>>>("SmkLogUnusedSection_Logs")
        val KEY = Key<HashSet<SmkRuleOrCheckpoint>>("SmkLogUnusedSection_Logs")
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SmkSLInspectionVisitor(holder, session) {
        override fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpressionImpl) {
            if (expr.isWildcard()) {
                return
            }

            val ruleOrCheckpointSection = expr.containingRuleOrCheckpointSection() ?: return
//            if (ruleOrCheckpointSection.name !in EXECUTION_SECTIONS_KEYWORDS
//                && ruleOrCheckpointSection.name != SECTION_RUN) {
//                return
//            }
            val ruleOrCheckpoint = ruleOrCheckpointSection.getParentRuleOrCheckPoint()
            val sectionsByRule = session.putUserDataIfAbsent(KEY, hashSetOf())
            if (ruleOrCheckpoint !in sectionsByRule) {
                val availableSections = ruleOrCheckpoint.getSections()
                    .asSequence()
                    .filterIsInstance(SmkRuleOrCheckpointArgsSection::class.java)
                    .filter { it.isWildcardsDefiningSection() }.firstOrNull() != null

                val wildcards = when {
                    // if no suitable sections let's think that no wildcards
                    !availableSections -> emptyList()
                    else -> {
                        // Cannot do via types, we'd like to have wildcards only from
                        // defining sections and ensure that defining sections could be parsed
                        val collector = SmkWildcardsCollector(
                            visitDefiningSections = true,
                            visitExpandingSections = false
                        )
                        ruleOrCheckpoint.accept(collector)
                        collector.getWildcardsNames()
                    }
                }
                sectionsByRule.add(ruleOrCheckpoint)
            }
        }
    }
}