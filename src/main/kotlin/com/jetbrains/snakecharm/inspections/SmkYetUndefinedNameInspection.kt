package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_CHECKPOINTS
import com.jetbrains.snakecharm.lang.SnakemakeNames.SMK_VARS_RULES
import com.jetbrains.snakecharm.lang.psi.SMKRule
import com.jetbrains.snakecharm.lang.psi.SMKRuleRunParameter

class SmkYetUndefinedNameInspection : SnakemakeInspection() {
    companion object {
        val INSPECTED_KEYWORDS = setOf(SMK_VARS_RULES, SMK_VARS_CHECKPOINTS)
    }

    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitPyReferenceExpression(node: PyReferenceExpression?) {
            val parentRun = PsiTreeUtil.getParentOfType(node, SMKRuleRunParameter::class.java)
            if (node?.text !in INSPECTED_KEYWORDS || parentRun != null) {
                return
            }

            val parent = node!!.parent
            val resolvedNode = parent.reference?.resolve() // Either corresponding rule or checkpoint or null

            if (resolvedNode != null && resolvedNode.containingFile == node.containingFile &&
                (resolvedNode.textOffset > parent.textOffset ||
                 resolvedNode === PsiTreeUtil.getParentOfType(node, SMKRule::class.java))) {
                val identifier = node.nextSibling.nextSibling
                registerProblem(identifier,
                        SnakemakeBundle.message("INSP.NAME.undefined.name") +
                        ": ${(node.parent as PyReferenceExpression).name}")
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.undefined.name")
}