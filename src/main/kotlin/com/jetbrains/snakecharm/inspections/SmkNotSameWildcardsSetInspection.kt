package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType.WEAK_WARNING
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.psi.util.parentOfType
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.*

class SmkNotSameWildcardsSetInspection : SnakemakeInspection() {
    companion object {
        val KEY = Key<HashMap<SmkRuleOrCheckpoint, Ref<List<WildcardDescriptor>>>>("SmkNotSameWildcardsSetInspection_Wildcards")
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        private val visitedRules = mutableSetOf<String>()

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
            val cachedWildcardsByRule = session.putUserDataIfAbsent(KEY, hashMapOf())
            val collector = AdvancedWildcardsCollector(
                visitDefiningSections = true,
                visitExpandingSections = false,
                ruleLike = ruleOrCheckpoint,
                cachedWildcardsByRule = cachedWildcardsByRule
            )

            val wildcards = collector.getDefinedWildcards().map { it.text }.toSet()
            val visitedSections = mutableSetOf<String>()
            handleSections(null, ruleOrCheckpoint, visitedSections, wildcards)
            if (ruleOrCheckpoint is SmkUse) {
                visitedRules.add(ruleOrCheckpoint.name ?: return)
                ruleOrCheckpoint.getImportedRuleNames()
                    ?.forEach { handleUseParent(ruleOrCheckpoint, it, visitedSections, wildcards) }
            }
        }

        private fun processSection(
            section: SmkRuleOrCheckpointArgsSection,
            wildcards: Set<String>,
            originalUseParent: SmkUse?
        ) {
            val sectionName = section.sectionKeyword ?: return
            val sectionArgs = section.argumentList?.arguments ?: return
            sectionArgs.forEach { arg ->
                val errorTarget = originalUseParent?.nameIdentifier ?: arg
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
                        val message = if (originalUseParent != null) {
                            SnakemakeBundle.message(
                                "INSP.NAME.not.same.wildcards.set.in.parent.rule",
                                missingWildcards.sorted().joinToString { "'$it'" },
                                sectionName
                            )
                        } else {
                            SnakemakeBundle.message(
                                "INSP.NAME.not.same.wildcards.set",
                                missingWildcards.sorted().joinToString { "'$it'" }
                            )
                        }
                        registerProblem(errorTarget, message)
                    }
                } else {
                    if (wildcards.isNotEmpty()) {
                        // no injections, cannot check
                        val message = if (originalUseParent != null) {
                            SnakemakeBundle.message(
                                "INSP.NAME.not.same.wildcards.set.cannot.check.in.parent.rule",
                                sectionName
                            )
                        } else {
                            SnakemakeBundle.message(
                                "INSP.NAME.not.same.wildcards.set.cannot.check"
                            )
                        }
                        registerProblem(errorTarget, message, WEAK_WARNING)
                    }
                }
            }
        }

        private fun handleUseParent(
            originalUseParent: SmkUse,
            parentRef: SmkReferenceExpression,
            visitedSections: MutableSet<String>,
            wildcards: Set<String>
        ) {
            var resolveResult = parentRef.reference.resolve()
            while (resolveResult is SmkReferenceExpression) {
                val parentUseSection = resolveResult.parentOfType<SmkUse>() ?: return
                handleSections(originalUseParent, parentUseSection, visitedSections, wildcards)
                resolveResult = resolveResult.reference.resolve()
            }
            if (resolveResult !is SmkUse && resolveResult is SmkRuleOrCheckpoint) {
                handleSections(originalUseParent, resolveResult, visitedSections, wildcards)
                return
            }
            val useParent =
                if (resolveResult is SmkUse) resolveResult else resolveResult?.parentOfType() ?: return
            val useName = useParent.name ?: return
            if(visitedRules.contains(useName)) {
                return
            }
            visitedRules.add(useName)
            handleSections(originalUseParent, useParent, visitedSections, wildcards)
            useParent.getImportedRuleNames()
                ?.forEach { handleUseParent(originalUseParent, it, visitedSections, wildcards) }
        }

        private fun handleSections(
            originalUseParent: SmkUse?,
            ruleLike: SmkRuleOrCheckpoint,
            visitedSections: MutableSet<String>,
            wildcards: Set<String>
        ) {
            ruleLike.getSections().forEach { section ->
                if (section.sectionKeyword in visitedSections || section !is SmkRuleOrCheckpointArgsSection || !section.isWildcardsDefiningSection()) {
                    return@forEach
                }
                visitedSections.add(section.sectionKeyword ?: return@forEach)
                processSection(section, wildcards, originalUseParent)
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.not.same.wildcards.set", "")
}
