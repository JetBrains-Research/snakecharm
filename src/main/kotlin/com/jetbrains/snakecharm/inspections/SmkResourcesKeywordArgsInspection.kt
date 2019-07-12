package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleArgsSection

class SmkResourcesKeywordArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSMKRuleParameterListStatement(st: SmkRuleArgsSection) {
            if(st.sectionKeyword != SnakemakeNames.SECTION_RESOURCES) {
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