package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.util.forEachDescendantOfType
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.*
import com.jetbrains.snakecharm.lang.psi.impl.SmkWorkflowLocalrulesSectionImpl

class SmkRulesOrderConfusingReference : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        fun visitSmkWorkflowRuleorderSection(st: SmkWorkflowLocalrulesSection) {
            val file = st.containingFile
            if (file !is SmkFile) {
                return
            }

            val args = st.argumentList?.arguments
                    ?.filterIsInstance(SmkReferenceExpression::class.java)

            if (args != null) {
                val rules = file.collectRules().map { it.first }
                val checkPoints = file.collectCheckPoints().map { it.first }
                val ruleLike = rules.plus(checkPoints).toSet()
                args.forEach { expr ->
                    val name = expr.name
                    if (name != null && name !in ruleLike) {
                        registerProblem(
                                expr,
                                SnakemakeBundle.message("INSP.NAME.rulesorder.confusing.ref.msg", name)
                        )
                    }
                }
            }
        }
    }
}