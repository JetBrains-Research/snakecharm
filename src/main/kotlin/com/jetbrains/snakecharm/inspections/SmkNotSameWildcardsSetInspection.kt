package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
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

        override fun visitSmkUse(use: SmkUse) {
            processRuleOrCheckpointLike(use)
        }

        fun processRuleOrCheckpointLike(ruleOrCheckpoint: SmkRuleOrCheckpoint) {
            val collector = AdvanceWildcardsCollector(
                visitDefiningSections = true,
                visitExpandingSections = false,
                ruleLike = ruleOrCheckpoint,
                collectDescriptors = true,
                cachedWildcardsByRule = null
            )

            val wildcards = collector.getDefinedWildcardDescriptors().map { it.text }.toSet()
            ruleOrCheckpoint.getSections().forEach { section ->
                if (section !is SmkRuleOrCheckpointArgsSection || !section.isWildcardsDefiningSection()) {
                    return@forEach
                }
                processSection(section, wildcards)
            }
        }

        private fun processSection(
            section: SmkRuleOrCheckpointArgsSection,
            wildcards: Set<String>
        ) {
            val sectionArgs = section.argumentList?.arguments ?: return
            sectionArgs.forEach { arg ->
                // collect wildcards in section arguments:
                val collector = SmkWildcardsCollector(
                    visitDefiningSections = false,
                    visitExpandingSections = false
                )
                arg.accept(collector)
                val argWildcards = collector.getWildcardsNames()

                if (argWildcards != null) {
                    // if wildcards defined for args section
                    val missingWildcards = wildcards.filter { it !in argWildcards }
                    if (missingWildcards.isNotEmpty()) {
                        registerProblem(
                            arg,
                            SnakemakeBundle.message(
                                "INSP.NAME.not.same.wildcards.set",
                                missingWildcards.sorted().joinToString() { "'$it'" }
                            )
                        )
                    }
                } else {
                    if (wildcards.isNotEmpty()) {
                        // no injections, cannot check
                        registerProblem(
                            arg,
                            SnakemakeBundle.message(
                                "INSP.NAME.not.same.wildcards.set.cannot.check"
                            ),
                            ProblemHighlightType.WEAK_WARNING
                        )
                    }
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.not.same.wildcards.set", "")
}
