package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLReferenceExpressionImpl

class SmkWildcardNotDefinedInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : AbstractSmkWildcardsInspectionVisitor<SmkSLReferenceExpressionImpl>(holder, session) {
        override fun visitPyReferenceExpression(expr: PyReferenceExpression) {
            if (expr !is SmkSLReferenceExpressionImpl || !expr.isWildcard()) {
                return
            }

            updateDeclarationAndWildcards(
                    expr,
                    getDeclaration = { expr.getContainingDeclaration() }
            )

            if (expr.text !in currentGeneratedWildcards) {
                val definingSectionName = currentDeclaration?.getWildcardDefiningSection()?.name
                val message =
                    if (definingSectionName == null) {
                        SnakemakeBundle.message("INSP.NAME.wildcard.not.defined", expr.text)
                    } else {
                        SnakemakeBundle.message(
                                "INSP.NAME.wildcard.not.defined.in.section",
                                expr.text,
                                definingSectionName
                        )
                    }

                registerProblem(expr, message)
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.wildcard.not.defined", "")
}
