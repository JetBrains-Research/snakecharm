package com.jetbrains.snakecharm.inspections.smksl

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiReference
import com.jetbrains.python.extensions.getQName
import com.jetbrains.python.psi.PyClass
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeApi.SECTION_ACCESSOR_CLASSES
import com.jetbrains.snakecharm.codeInsight.SnakemakeApiService
import com.jetbrains.snakecharm.inspections.SnakemakeInspection
import com.jetbrains.snakecharm.inspections.smksl.SmkSLUndeclaredSectionInspectionUtil.checkIsSectionNameUnresolved
import com.jetbrains.snakecharm.inspections.smksl.SmkSLUndeclaredSectionInspectionUtil.isSectionNameOfInterest
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.SnakemakeNames.SNAKEMAKE_MODULE_NAME_IO
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLInitialReference

class SmkSLUndeclaredSectionInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SmkSLInspectionVisitor(holder, getContext(session)) {
        val api = SnakemakeApiService.getInstance(holder.project)

        override fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpression) {
            val ref = expr.reference
            if (ref is SmkSLInitialReference) {
                @Suppress("UnstableApiUsage")
                val referencedName = expr.referencedName

                val ruleOrCheckPoint = expr.containingSection()?.getParentRuleOrCheckPoint()
                if (ruleOrCheckPoint == null) {
                    // ensure in rule
                    return
                }
                val contextKeyword = ruleOrCheckPoint.sectionKeyword
                if (isSectionNameOfInterest(referencedName, contextKeyword, api)) {
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

        return when {
            (declaration == null) || (declaration is SmkRuleOrCheckpoint) -> {
                // 'thread' has a default value => allow unresolved
                // Undeclared sections could be not resolved (SmkSL) or resolved into enire Rule (run sections)
                ref.element.text != SnakemakeNames.SECTION_THREADS
            }
            declaration is PyClass -> {
                // is resolved to snakemake/io.py
                declaration.containingFile.getQName()?.toString() == SNAKEMAKE_MODULE_NAME_IO
                        || declaration.qualifiedName in SECTION_ACCESSOR_CLASSES
            }

            else -> false
        }
    }

    fun isSectionNameOfInterest(referencedName: String?, contextKeyword: String?, api: SnakemakeApiService) =
        api.isSubsectionAccessibleAsPlaceholder(referencedName, contextKeyword)
}
