package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkRunSection
import com.jetbrains.snakecharm.lang.psi.types.SmkCheckpointType
import com.jetbrains.snakecharm.lang.psi.types.SmkRulesType

class SmkRuleOrCheckpointNameYetUndefinedInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

        override fun visitPyReferenceExpression(node: PyReferenceExpression) {
            val nodeFile = node.containingFile
            val type = TypeEvalContext.codeAnalysis(node.project, nodeFile).getType(node)
            if (type !is SmkRulesType && type !is SmkCheckpointType) {
                return
            }

            val containingRunSection = PsiTreeUtil.getParentOfType(node, SmkRunSection::class.java)
            if (containingRunSection != null) {
                return
            }

            val containingLambdaSection = PsiTreeUtil.getParentOfType(node, PyLambdaExpression::class.java)
            if (containingLambdaSection != null) {
                // you could use lambdas for rules which are defined later
                return
            }

            // Expected that parent is rule name, e.g. `foo` in `rules.foo` reference:
            val ruleNameReference = node.parent as? PyReferenceExpression ?: return

            // Either corresponding rule or checkpoint or null
            val target = ruleNameReference.reference.resolve()
            val targetRuleLikeElem = target as? SmkRuleOrCheckpoint ?: return

            // parent is
            val ruleNameOffset = ruleNameReference.textOffset

            val targetFile = targetRuleLikeElem.containingFile
            val targetOffset = targetRuleLikeElem.textOffset

            val nodeRuleLikeElem = PsiTreeUtil.getParentOfType(
                node, SmkRuleOrCheckpoint::class.java
            )

            if (targetFile == nodeFile
                && (targetOffset > ruleNameOffset || targetRuleLikeElem === nodeRuleLikeElem)
            ) {

                val message = SnakemakeBundle.message(
                    "INSP.NAME.rule.or.checkpoint.name.yet.undefined.msg",
                    targetRuleLikeElem.name ?: "n/a"
                )
                registerProblem(ruleNameReference.nameElement?.psi, message)
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.rule.or.checkpoint.name.yet.undefined")
}