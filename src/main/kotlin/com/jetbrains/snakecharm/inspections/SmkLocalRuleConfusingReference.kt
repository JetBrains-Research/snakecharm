package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.SmkReferenceExpression
import com.jetbrains.snakecharm.lang.psi.SmkWorkflowLocalrulesSection

class SmkLocalRuleConfusingReference : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSmkWorkflowLocalrulesSection(st: SmkWorkflowLocalrulesSection) {
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
                            SnakemakeBundle.message("INSP.NAME.localrules.confusing.ref.msg", name)
                        )
                    }
                }
            }
        }
    }
}