package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.inspections.smksl.SmkSLUndeclaredSectionInspection
import com.jetbrains.snakecharm.lang.psi.impl.refs.SmkPyReferenceImpl

class SmkUndeclaredSectionInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitPyReferenceExpression(expr: PyReferenceExpression) {
            val ref = expr.reference

            @Suppress("FoldInitializerAndIfToElvis")
            if (ref !is SmkPyReferenceImpl) {
                return
            }
            if (!ref.inRunSection) {
                return
            }

            val referencedName = expr.referencedName
            if (!SmkSLUndeclaredSectionInspection.isSectionNameOfInterest(referencedName)) {
                return
            }
                
            if (SmkSLUndeclaredSectionInspection.checkIsSectionNameUnresolved(ref)) {
                registerProblem(
                        expr,
                        SnakemakeBundle.message("INSP.NAME.undeclared.section.message", referencedName!!)
                )
            }
        }
    }
}