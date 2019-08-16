package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.resolve.SmkResolveUtil
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkReferenceExpression
import com.jetbrains.snakecharm.lang.psi.SmkWorkflowLocalrulesSection
import com.jetbrains.snakecharm.lang.psi.SmkWorkflowRuleorderSection

// TODO should we highlight in case of multiple rules with same name when one is present in current file
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
            val potentiallyUnresolvedReferences = references.filterNot { ref ->
                val ruleOrCheckpoint = ref.reference?.resolve()
                ruleOrCheckpoint?.containingFile == file || ruleOrCheckpoint?.containingFile in includedFiles
            }
            potentiallyUnresolvedReferences.forEach { registerProblem(
                    it, SnakemakeBundle.message("INSP.NAME.rule.not.included"), ProblemHighlightType.WEAK_WARNING
            ) }
        }
    }
}