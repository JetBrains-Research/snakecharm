package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.Ref
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.impl.PyCallExpressionImpl
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_FUN_EXPAND
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SMK_VARS_RULES
import com.jetbrains.snakecharm.inspections.smksl.SmkWildcardNotDefinedInspection
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkWildcardsCollector
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes.SMK_PY_REFERENCE_EXPRESSION

class SmkWildcardNotDefinedInExpressionInspection : SnakemakeInspection() {

    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {


        override fun visitPyReferenceExpression(node: PyReferenceExpression?) {

            if (node?.text != SMK_VARS_RULES) {
                return
            }

            val callExpression = PsiTreeUtil.getParentOfType(node, PyCallExpressionImpl::class.java)

            if (callExpression?.firstChild?.text == SMK_FUN_EXPAND) {
                return
            }

            var lastReference = node

            while (lastReference?.parent.elementType == SMK_PY_REFERENCE_EXPRESSION) {
                lastReference = lastReference?.parent as PyReferenceExpression?
            }

            val reference = lastReference?.reference?.resolve()

            var collector = SmkWildcardsCollector(
                    visitDefiningSections = false,
                    visitExpandingSections = true
            )

            reference?.accept(collector)

            val necessaryWildcards = collector.getWildcardsNames()

            val ruleOrCheckpoint = PsiTreeUtil.getParentOfType(node, SmkRuleOrCheckpoint::class.java) ?: return

            val wildcardsByRule = session.putUserDataIfAbsent(SmkWildcardNotDefinedInspection.KEY, hashMapOf())

            if (ruleOrCheckpoint !in wildcardsByRule) {
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
                        collector = SmkWildcardsCollector(
                                visitDefiningSections = true,
                                visitExpandingSections = false
                        )

                        ruleOrCheckpoint.accept(collector)
                        collector.getWildcardsNames()
                    }
                }
                wildcardsByRule[ruleOrCheckpoint] =  Ref.create(wildcards)
            }
            val availableWildcards = wildcardsByRule.getValue(ruleOrCheckpoint).get()

            necessaryWildcards?.forEach { wildcard ->
                if (wildcard !in availableWildcards){
                    if (lastReference != null) {
                        registerProblem(
                                lastReference.lastChild,
                                SnakemakeBundle.message("INSP.NAME.wildcard.not.defined.in.expression", wildcard)
                        )
                    }
                }
            }
        }
    }
}
