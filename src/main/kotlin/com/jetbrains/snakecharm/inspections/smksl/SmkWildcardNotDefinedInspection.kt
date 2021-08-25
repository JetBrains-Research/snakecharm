package com.jetbrains.snakecharm.inspections.smksl

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
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
            val advancedCollector = AdvanceWildcardsCollector(
                visitDefiningSections = true,
                visitExpandingSections = false,
                ruleLike = ruleLikeBlock,
                cachedWildcardsByRule = cachedWildcardsByRule
            )
            val wildcards = advancedCollector.getDefinedWildcards()
            val wildcardsCollectedInAllOverriddenRules = advancedCollector.wildcardsCollectedInAllOverriddenRules()
            val inUseRuleBlock = ruleLikeBlock is SmkUse

            //val wildcards = cachedWildcardsByRule.getValue(ruleLikeBlock).get()
            // If an appropriate wildcard exists
            if (expr.text in wildcards) {
                return
            }
            // If no, firstly we need to check if there are failures to wildcard collecting
            // If so, adds weak warning
            if (!wildcardsCollectedInAllOverriddenRules) {
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
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.wildcard.not.defined", "")
}