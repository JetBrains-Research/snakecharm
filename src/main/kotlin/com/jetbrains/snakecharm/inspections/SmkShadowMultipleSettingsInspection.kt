package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement

class SmkShadowMultipleSettingsInspection : SnakemakeInspection()  {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitSMKRuleParameterListStatement(st: SMKRuleParameterListStatement) {
            if (st.name != SMKRuleParameterListStatement.SHADOW) {
                return
            }

            val size = st.argumentList?.arguments?.size
            if (size != null && size > 1) {
                st.argumentList!!.arguments.forEach {
                    registerProblem(it,
                            SnakemakeBundle.message("INSP.NAME.shadow.multiple.settings"))
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.shadow.multiple.settings")
}