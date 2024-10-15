package com.jetbrains.snakecharm.inspections.smksl

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.inspections.quickfix.PyRenameElementQuickFix
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPIProjectService
import com.jetbrains.snakecharm.inspections.SnakemakeInspection
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLWildcardReference

class SmkSLWildcardNameIsConfusingInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SmkSLInspectionVisitor(holder, getContext(session)) {
        val api = SnakemakeAPIProjectService.getInstance(holder.project)

        override fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpression) {
            // expr.isQualified: 'wildcards' in 'wildcards.input'

            val ref = expr.reference
            if (ref is SmkSLWildcardReference) {
                val wildcardName = ref.wildcardName()
                val ruleOrCheckPoint = expr.containingSection()?.getParentRuleOrCheckPoint()
                if (ruleOrCheckPoint == null) {
                    return
                }
                val contextKeyword = ruleOrCheckPoint.sectionKeyword
                if (api.isSubsectionAccessibleAsPlaceholder(wildcardName, contextKeyword)) {
                    // ensure in rule
                    ruleOrCheckPoint ?: return

                    registerProblem(
                        expr,
                        SnakemakeBundle.message(
                            "INSP.NAME.wildcards.confusing.name.like.section.message",
                            wildcardName
                        ),
                        ProblemHighlightType.WARNING,
                        null,
                        PyRenameElementQuickFix(expr),
                    )
                } else if ('.' in wildcardName) {
                    // E.g. 'wildcards.name' or 'foo.boo.doo'
                    registerProblem(
                        ref.getWildcardTrueExpression(),
                        SnakemakeBundle.message("INSP.NAME.wildcards.confusing.name.with.dot.message", wildcardName)
                    )
                }
            }
        }
    }
}

