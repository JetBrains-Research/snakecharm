package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.*

class SmkNotSameWildcardsSetInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSmkRule(rule: SmkRule) {
            processRuleOrCheckpointLike(rule)
        }

        override fun visitSmkCheckPoint(checkPoint: SmkCheckPoint) {
            processRuleOrCheckpointLike(checkPoint)
        }

        fun processRuleOrCheckpointLike(ruleOrCheckpoint: SmkRuleOrCheckpoint) {
            var wildcards: List<String>? = null

            ruleOrCheckpoint.getSections().forEach { section ->
                if (section !is SmkRuleOrCheckpointArgsSection) {
                    return@forEach
                }

                // consider non-output wildcards defining sections:
                val sectionKeyword = section.sectionKeyword
                if (sectionKeyword == SnakemakeNames.SECTION_OUTPUT ||
                        sectionKeyword !in SmkRuleOrCheckpointArgsSection.SECTIONS_DEFINING_WILDCARDS) {
                    return@forEach
                }

                if (wildcards == null) {
                    wildcards = ruleOrCheckpoint.collectWildcards().map { it.second }
                }

                processSection(section, wildcards!!)
            }
        }

        private fun processSection(section: SmkRuleOrCheckpointArgsSection, ruleWildcards: List<String>) {

            val sectionArgs = section.argumentList?.arguments ?: return
            sectionArgs.forEach { arg ->
                val argWildcards = SmkWildcardsCollector().also {
                    arg.accept(it)
                }.getWildcards().asSequence().map { it.second }.distinct().toList()

                val missingWildcards = ruleWildcards.filter { it !in argWildcards }
                if (missingWildcards.isNotEmpty()) {
                    registerProblem(
                            arg,
                            SnakemakeBundle.message(
                                    "INSP.NAME.not.same.wildcards.set", missingWildcards.joinToString()
                            )
                    )
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.not.same.wildcards.set", "")
}
