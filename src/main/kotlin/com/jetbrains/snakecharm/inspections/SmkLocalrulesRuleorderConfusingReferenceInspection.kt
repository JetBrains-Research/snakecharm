package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.*

class SmkLocalrulesRuleorderConfusingReferenceInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

        override fun visitSmkWorkflowLocalrulesSection(st: SmkWorkflowLocalrulesSection) {
            checkConfusingReference(st)
        }

        override fun visitSmkWorkflowRuleorderSection(st: SmkWorkflowRuleorderSection) {
            checkConfusingReference(st)
        }

        private fun checkConfusingReference(st: SmkArgsSection){
            val file = st.containingFile
            if (file !is SmkFile) {
                return
            }

            val args = st.argumentList?.arguments
                    ?.filterIsInstance(SmkReferenceExpression::class.java)

            if (args != null) {
                val ruleLike = file.collectRules(mutableSetOf()).map { it.first }.toSet()
                args.forEach { expr ->
                    @Suppress("UnstableApiUsage")
                    val name = expr.name
                    if (name != null && name !in ruleLike) {
                        registerProblem(
                                expr,
                                SnakemakeBundle.message("INSP.NAME.localrules.ruleorder.confusing.ref.msg", name)
                        )
                    }
                }
            }
        }
    }
}