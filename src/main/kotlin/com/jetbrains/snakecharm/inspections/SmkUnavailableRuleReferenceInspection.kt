package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiPolyVariantReference
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.psi.*

class SmkUnavailableRuleReferenceInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSmkWorkflowLocalrulesSection(st: SmkWorkflowLocalrulesSection) {
            val references = st.argumentList?.arguments?.filterIsInstance<SmkReferenceExpression>() ?: return
            checkUnresolvedReferences(references, st.containingFile as SmkFile)
        }

        override fun visitSmkWorkflowRuleorderSection(st: SmkWorkflowRuleorderSection) {
            val references = st.argumentList?.arguments?.filterIsInstance<SmkReferenceExpression>() ?: return
            checkUnresolvedReferences(references, st.containingFile as SmkFile)
        }

        private fun checkUnresolvedReferences(references: List<SmkReferenceExpression>, file: SmkFile) {
            val includedFiles = SmkResolveUtil.getIncludedFiles(file)
            val potentiallyUnresolvedReferences = references.filter {
                val rules = (it.reference as? PsiPolyVariantReference)
                        ?.multiResolve(false)
                        ?.mapNotNull { res -> res.element }
                rules?.all { rule -> !(rule.containingFile == file || rule.containingFile in includedFiles) } ?: false
            }
            val overriddenRulesOrCheckpoints = references.filter {
                val rules = (it.reference as? PsiPolyVariantReference)
                        ?.multiResolve(false)
                        ?.mapNotNull { res -> res.element }
                        ?.filter { rule -> rule.containingFile == file || rule.containingFile in includedFiles }
                rules != null && rules.size > 1
            }
            overriddenRulesOrCheckpoints.forEach {
                registerProblem(
                        it,
                        SnakemakeBundle.message("INSP.NAME.rule.redeclaration"),
                        ProblemHighlightType.GENERIC_ERROR
                )
            }

            potentiallyUnresolvedReferences.forEach { registerProblem(
                    it, SnakemakeBundle.message("INSP.NAME.rule.not.included"), ProblemHighlightType.WEAK_WARNING
            ) }
        }
    }
}