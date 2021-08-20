package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.Ref
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
            var wildcardsRef: Ref<List<String>?>? = null

            ruleOrCheckpoint.getSections().forEach { section ->
                if (section !is SmkRuleOrCheckpointArgsSection || !section.isWildcardsDefiningSection()) {
                    return@forEach
                }

                if (wildcardsRef == null) {
                    // Cannot do via types, we'd like to have wildcards only from
                    // defining sections and ensure that defining sections could be parsed
                    val collector = SmkWildcardsCollector(
                        visitDefiningSections = true,
                        visitExpandingSections = false
                    )
                    ruleOrCheckpoint.accept(collector)
                    wildcardsRef = Ref.create(collector.getWildcardsNames())
                }

                val wildcards = wildcardsRef!!.get()
                if (wildcards != null) {
                    // show warnings only if wildcards defined
                    processSection(section, wildcards)
                }
            }
        }

        private fun processSection(
            section: SmkRuleOrCheckpointArgsSection,
            wildcards: List<String>
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
