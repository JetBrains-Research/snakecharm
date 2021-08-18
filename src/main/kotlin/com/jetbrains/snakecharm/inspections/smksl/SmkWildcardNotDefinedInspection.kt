package com.jetbrains.snakecharm.inspections.smksl

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.psi.util.parentOfType
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.inspections.SnakemakeInspection
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpression

class SmkWildcardNotDefinedInspection : SnakemakeInspection() {
    companion object {
        val KEY = Key<HashMap<SmkRuleOrCheckpoint, Ref<List<String>>>>("SmkWildcardNotDefinedInspection_Wildcards")
    }

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SmkSLInspectionVisitor(holder, session) {

        override fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpression) {
            if (!expr.isWildcard()) {
                return
            }

            val ruleLikeBlock = expr.containingRuleOrCheckpointSection()?.getParentRuleOrCheckPoint() ?: return
            val cachedWildcardsByRule = session.putUserDataIfAbsent(KEY, hashMapOf())

            if (ruleLikeBlock !in cachedWildcardsByRule) {
                updateInfo(ruleLikeBlock, cachedWildcardsByRule)
            }

            var inUseRuleBlock = false
            var wildcardDefinedInAllOverriddenRules = false
            var wildcardsCollectedInAllOverriddenRules = true

            if (ruleLikeBlock is SmkUse) {
                inUseRuleBlock = true

                // Collecting wildcards from overridden rules or checkpoints
                val useReferences = mutableListOf<SmkRuleOrCheckpoint>()
                ruleLikeBlock.getImportedRuleNames()?.forEach { collectReferences(it, useReferences) }
                useReferences.forEach { targetRuleLike ->
                    if (targetRuleLike !in cachedWildcardsByRule) {
                        updateInfo(targetRuleLike, cachedWildcardsByRule)
                    }

                    val targetWildcards = cachedWildcardsByRule.getValue(targetRuleLike).get()
                    when (targetWildcards) {
                        null -> wildcardsCollectedInAllOverriddenRules = false
                        else -> {
                            wildcardDefinedInAllOverriddenRules = wildcardDefinedInAllOverriddenRules or
                                    (expr.text in targetWildcards)
                        }
                    }
                }
            }

            val wildcards = cachedWildcardsByRule.getValue(ruleLikeBlock).get()
            // If an appropriate wildcard exists
            if ((wildcards != null && expr.text in wildcards) || wildcardDefinedInAllOverriddenRules) {
                return
            }
            // If no, firstly we need to check if there are failures to wildcard collecting
            // If so, adds weak warning
            if (wildcards == null || !wildcardsCollectedInAllOverriddenRules) {
                // failed to parse wildcards defining sections
                registerProblem(
                    expr,
                    SnakemakeBundle.message("INSP.NAME.wildcard.not.defined.cannot.check", expr.text),
                    ProblemHighlightType.WEAK_WARNING
                )
            } else {
                // Otherwise adds an error
                val definingSection = ruleLikeBlock.getWildcardDefiningSection()?.sectionKeyword
                val message = when {
                    inUseRuleBlock -> {
                        SnakemakeBundle.message(
                            "INSP.NAME.wildcard.not.defined.in.overridden.rule",
                            expr.text
                        )
                    }
                    definingSection == null -> {
                        SnakemakeBundle.message("INSP.NAME.wildcard.not.defined", expr.text)
                    }
                    else -> {
                        SnakemakeBundle.message(
                            "INSP.NAME.wildcard.not.defined.in.section",
                            expr.text, definingSection
                        )
                    }
                }
                registerProblem(expr, message)
            }
        }

        private fun updateInfo(
            ruleOrCheckpoint: SmkRuleOrCheckpoint,
            wildcardsByRule: HashMap<SmkRuleOrCheckpoint, Ref<List<String>>>,
        ) {
            val wildcardsDefiningSectionsAvailable = ruleOrCheckpoint.getSections()
                .asSequence()
                .filterIsInstance(SmkRuleOrCheckpointArgsSection::class.java)
                .filter { it.isWildcardsDefiningSection() }.firstOrNull() != null

            val wildcards = when {
                // if no suitable sections let's think that no wildcards
                !wildcardsDefiningSectionsAvailable -> emptyList()
                else -> {
                    // Cannot do via types, we'd like to have wildcards only from
                    // defining sections and ensure that defining sections could be parsed
                    val collector = SmkWildcardsCollector(
                        visitDefiningSections = true,
                        visitExpandingSections = false
                    )
                    ruleOrCheckpoint.accept(collector)
                    collector.getWildcardsNames()
//
//                        val type = TypeEvalContext.codeAnalysis(expr.project, expr.containingFile)
//                                .getType(ruleOrCheckpoint.wildcardsElement)
//
//                        var definedWildcards: List<WildcardDescriptor>? = null
//                        if (type is SmkWildcardsType) {
//                            definedWildcards = type.wildcardsDeclarations
//                                    ?.filter { it.definingSectionRate != UNDEFINED_SECTION }
//                                    ?.ifEmpty { null }
//                        }
//                        definedWildcards
                }
            }
            wildcardsByRule[ruleOrCheckpoint] = Ref.create(wildcards)
        }

        private fun collectReferences(reference: SmkReferenceExpression, list: MutableList<SmkRuleOrCheckpoint>) {
            var resolveResult = reference.reference.resolve()
            while (resolveResult is SmkReferenceExpression) {
                val newUseRule = resolveResult.parentOfType<SmkUse>() ?: return
                list.add(newUseRule)
                resolveResult = resolveResult.reference.resolve()
            }
            if (resolveResult !is SmkUse && resolveResult is SmkRuleOrCheckpoint) {
                list.add(resolveResult)
            } else {
                val newUseRule =
                    (resolveResult as? SmkUse) ?: resolveResult?.parentOfType() ?: return
                list.add(newUseRule)
                newUseRule.getImportedRuleNames()?.forEach { collectReferences(it, list) }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.wildcard.not.defined", "")
}