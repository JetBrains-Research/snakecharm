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
import com.jetbrains.snakecharm.inspections.smksl.SmkSLUndeclaredSectionInspectionUtil.checkIsSectionNameUnresolved
import com.jetbrains.snakecharm.inspections.smksl.SmkSLUndeclaredSectionInspectionUtil.isSectionNameOfInterest
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLInitialReference

class SmkSLUndeclaredSectionInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SmkSLInspectionVisitor(holder, getContext(session)) {

        override fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpression) {
            val ref = expr.reference
            if (ref is SmkSLInitialReference) {
                @Suppress("UnstableApiUsage")
                val referencedName = expr.referencedName
                if (isSectionNameOfInterest(referencedName)) {
                    // ensure in rule
                    expr.containingSection()?.getParentRuleOrCheckPoint() ?: return

                    if (checkIsSectionNameUnresolved(ref)) {
                        registerProblem(
                            expr,
                            SnakemakeBundle.message("INSP.NAME.undeclared.section.message", referencedName!!)
                        )
                    }
                }
            }
        }
    }
}

object SmkSLUndeclaredSectionInspectionUtil {
    fun checkIsSectionNameUnresolved(ref: PsiReference): Boolean {
        val declaration = ref.resolve()

        return when (declaration) {
            null -> true
            is PyClass -> {
                // is resolved to io.py
                declaration.containingFile.name == SNAKEMAKE_MODULE_NAME_IO_PY
                        || declaration.qualifiedName in SECTION_ACCESSOR_CLASSES
            }

            is SmkRuleOrCheckpoint -> true
            else -> false
        }
    }

    fun isSectionNameOfInterest(referencedName: String?) =
        referencedName in SnakemakeAPI.SMK_SL_INITIAL_TYPE_ACCESSIBLE_SECTIONS
}
