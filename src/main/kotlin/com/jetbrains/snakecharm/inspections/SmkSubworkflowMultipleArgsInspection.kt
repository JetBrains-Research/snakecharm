package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SMKSubworkflowParameterListStatement

class SmkSubworkflowMultipleArgsInspection  : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitSMKSubworkflowParameterListStatement(st: SMKSubworkflowParameterListStatement) {
            val args = st.argumentList?.arguments ?: emptyArray()
            if (args.size > 1) {
                args.forEach {
                    registerProblem(it,
                            SnakemakeBundle.message("INSP.NAME.subworkflow.multiple.args"))
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.subworkflow.multiple.args")
}