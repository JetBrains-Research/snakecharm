package com.jetbrains.snakecharm.inspections.smksl

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiReference
import com.jetbrains.python.psi.PyClass
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SECTION_ACCESSOR_CLASSES
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SNAKEMAKE_MODULE_NAME_IO_PY
import com.jetbrains.snakecharm.inspections.SnakemakeInspection
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpressionImpl
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLInitialReference

class SmkSLUnresolvedSectionInspection: SnakemakeInspection() {
    override fun buildVisitor(
                holder: ProblemsHolder,
                isOnTheFly: Boolean,
                session: LocalInspectionToolSession
        ) = object : SmkSLInspectionVisitor(holder, session) {
        
        override fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpressionImpl) {
            val ref = expr.reference
            if (ref is SmkSLInitialReference) {
                val referencedName = expr.referencedName
                if (isSectionNameOfInterest(referencedName)) {
                    // ensure in rule
                    expr.containingSection()?.getParentRuleOrCheckPoint() ?: return

                    if (checkIsSectionNameUnresolved(ref)) {
                        registerProblem(
                                expr,
                                SnakemakeBundle.message("INSP.NAME.unresolved.section.message", referencedName!!)
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun checkIsSectionNameUnresolved(ref: PsiReference): Boolean {
            val declaration = ref.resolve()

            return when (declaration) {
                null -> true
                is PyClass -> {
                    // is resolved to io.py
                    declaration.containingFile.name == SNAKEMAKE_MODULE_NAME_IO_PY
                            || declaration.qualifiedName in SECTION_ACCESSOR_CLASSES
                }
                else -> false
            }
        }

        fun isSectionNameOfInterest(referencedName: String?) =
                referencedName in SnakemakeAPI.SMK_SL_INITIAL_TYPE_ACCESSIBLE_SECTIONS
    }
}