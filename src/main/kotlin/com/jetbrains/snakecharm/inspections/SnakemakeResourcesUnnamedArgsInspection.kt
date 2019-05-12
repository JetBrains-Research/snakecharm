package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement

class SnakemakeResourcesUnnamedArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSMKRuleParameterListStatement(st: SMKRuleParameterListStatement) {
            if (st.containingFile.language !is SnakemakeLanguageDialect) {
                return
            }

            if (st.sectionName != SMKRuleParameterListStatement.RESOURCES) {
                return
            }

            st.argumentList?.arguments?.forEach {
                if (it !is PyKeywordArgument) {
                    registerProblem(it, SnakemakeBundle.message("INSP.NAME.resources.unnamed.args"))
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.resources.unnamed.args")
}