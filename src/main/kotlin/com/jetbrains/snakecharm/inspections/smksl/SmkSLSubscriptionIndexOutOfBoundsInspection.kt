package com.jetbrains.snakecharm.inspections.smksl

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.inspections.SnakemakeInspection
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLSubscriptionIndexKeyExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLSubscriptionKeyReference

class SmkSLSubscriptionIndexOutOfBoundsInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SmkSLInspectionVisitor(holder, session) {

        override fun visitSmkSLSubscriptionExpressionKey(expr: SmkSLSubscriptionIndexKeyExpression) {
            val ref = expr.reference
            if (ref is SmkSLSubscriptionKeyReference) {
                val refText = ref.canonicalText
                val idx = refText.toIntOrNull() ?: return
                val type = ref.type ?: return

                val errorMsg = checkOutOfBounds(type, expr, idx)
                if (errorMsg != null) {
                    registerProblem(expr, errorMsg)
                }

            }
        }
    }

    companion object {
        fun checkOutOfBounds(
            type: SmkAvailableForSubscriptionType,
            expr: PsiElement,
            idx: Int,
        ): String? {
            val argsNumber = type.getPositionArgsNumber(expr)

            if (argsNumber > 0) {
                if (idx >= argsNumber || idx < 0) {
                    return if (argsNumber == 1) {
                        SnakemakeBundle.message("INSP.NAME.section.arg.idx.aiobe.zero.message")
                    } else {
                        SnakemakeBundle.message(
                            "INSP.NAME.section.arg.idx.aiobe.message", argsNumber - 1
                        )
                    }
                }
            }
            return null
        }
    }
}