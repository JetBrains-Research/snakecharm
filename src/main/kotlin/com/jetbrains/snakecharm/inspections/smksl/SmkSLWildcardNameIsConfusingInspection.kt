package com.jetbrains.snakecharm.inspections.smksl

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.inspections.quickfix.PyRenameElementQuickFix
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import com.jetbrains.snakecharm.inspections.SnakemakeInspection
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLLanguageElement
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpressionImpl
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLWildcardReference

class SmkSLWildcardNameIsConfusingInspection : SnakemakeInspection() {
    override fun buildVisitor(
                holder: ProblemsHolder,
                isOnTheFly: Boolean,
                session: LocalInspectionToolSession
        ) = object : SmkSLInspectionVisitor(holder, session) {

        override fun visitSmkSLLanguageElement(element: SmkSLLanguageElement) {
            element.getExpressions().forEach {expr ->
                if (expr is SmkSLReferenceExpressionImpl) {
                    process(expr)
                }
            }
            super.visitSmkSLLanguageElement(element)
        }

        private fun process(expr: SmkSLReferenceExpressionImpl) {
            val ref = expr.reference
            if (ref is SmkSLWildcardReference) {
                val wildcardName = ref.wildcardName()
                if (wildcardName in SnakemakeAPI.SMK_SL_INITIAL_TYPE_ACCESSIBLE_SECTIONS) {
                    // ensure in rule
                    expr.containingSection()?.getParentRuleOrCheckPoint() ?: return

                    registerProblem(
                            expr,
                            SnakemakeBundle.message("INSP.NAME.wildcards.confusing.section.message", wildcardName),
                            PyRenameElementQuickFix(expr)
                    )
                }
            }
        }
    }
}

