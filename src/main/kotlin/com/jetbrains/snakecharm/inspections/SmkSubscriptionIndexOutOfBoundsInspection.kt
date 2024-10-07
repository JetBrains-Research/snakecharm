package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyNumericLiteralExpression
import com.jetbrains.python.psi.PySubscriptionExpression
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.inspections.smksl.SmkSLInspectionVisitor
import com.jetbrains.snakecharm.inspections.smksl.SmkSLSubscriptionIndexOutOfBoundsInspection
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType

class SmkSubscriptionIndexOutOfBoundsInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SmkSLInspectionVisitor(holder, getContext(session)) {
        override fun visitPySubscriptionExpression(expr: PySubscriptionExpression) {
            val psiOperand = expr.operand
            val indexExp = expr.indexExpression ?: return

            // negative numbers not supported
            if (indexExp is PyNumericLiteralExpression) {
                @Suppress("UnstableApiUsage")
                val idx = indexExp.longValue?.toInt() ?: return

                PsiTreeUtil.getParentOfType(expr, SmkRuleOrCheckpoint::class.java) ?: return

                val type = TypeEvalContext.codeAnalysis(expr.project, expr.containingFile).getType(psiOperand)
                if (type is SmkAvailableForSubscriptionType) {
                    val errorMsg = SmkSLSubscriptionIndexOutOfBoundsInspection.checkOutOfBounds(type, expr, idx)
                    if (errorMsg != null) {
                        registerProblem(indexExp, errorMsg)
                    }

                }
            }
        }
    }
}