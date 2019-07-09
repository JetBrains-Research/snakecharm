package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames

class SmkYetUndefinedNameInspection : SnakemakeInspection() {
    companion object {
        val INSPECTED_KEYWORDS = setOf(
                SnakemakeNames.RULE_KEYWORD + "s",
                SnakemakeNames.CHECKPOINT_KEYWORD + "s")
    }

    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitPyReferenceExpression(node: PyReferenceExpression?) {
            if (node?.text !in INSPECTED_KEYWORDS) {
                return
            }

            val parent = node!!.parent
            val resolvedNode = parent.reference?.resolve() // Either corresponding rule or checkpoint or null

            if (resolvedNode != null && resolvedNode.textOffset > parent.textOffset) {
                registerProblem(parent,SnakemakeBundle.message("INSP.NAME.undefined.name") +
                                ": ${(node.parent as PyReferenceExpression).name}")
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.undefined.name")
}