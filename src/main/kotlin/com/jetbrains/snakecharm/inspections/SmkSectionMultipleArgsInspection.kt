package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SINGLE_ARGUMENT_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflowArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkWorkflowArgsSection

class SmkSectionMultipleArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
            checkArgumentList(st.argumentList, "subworkflow")
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            if (st.name in SINGLE_ARGUMENT_SECTIONS_KEYWORDS) {
                checkArgumentList(st.argumentList, st.name!!)
            }
        }

        override fun visitSmkWorkflowArgsSection(st: SmkWorkflowArgsSection) {
            val name = st.sectionKeyword
            if (name != null && name in SINGLE_ARGUMENT_SECTIONS_KEYWORDS) {
                checkArgumentList(st.argumentList, name)
            }
        }

        private fun checkArgumentList(
                argumentList: PyArgumentList?,
                sectionName: String
        ) {
            val args = argumentList?.arguments ?: emptyArray()
            if (args.size > 1) {
                args.forEachIndexed { i, arg ->
                    if (i > 0) {
                        registerProblem(
                                arg,
                                SnakemakeBundle.message("INSP.NAME.section.multiple.args.message", sectionName)
                        )
                    }
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.section.multiple.args")
}