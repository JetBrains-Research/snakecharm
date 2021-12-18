package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SINGLE_ARGUMENT_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SINGLE_ARGUMENT_WORKFLOWS_KEYWORDS
import com.jetbrains.snakecharm.lang.psi.*

class SmkSectionMultipleArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

        override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
            checkArgumentList(st.argumentList, "subworkflow")
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            checkArgumentList(st, SINGLE_ARGUMENT_SECTIONS_KEYWORDS)
        }

        override fun visitSmkWorkflowArgsSection(st: SmkWorkflowArgsSection) {
            checkArgumentList(st, SINGLE_ARGUMENT_WORKFLOWS_KEYWORDS)
        }

        private fun checkArgumentList(st: SmkArgsSection, sectionKeywords: Set<String>) {
            val keyword = st.sectionKeyword
            if (keyword != null && keyword in sectionKeywords) {
                checkArgumentList(st.argumentList, keyword)
            }
        }

        override fun visitSmkModuleArgsSection(st: SmkModuleArgsSection) {
            checkArgumentList(st.argumentList, "module")
        }

        private fun checkArgumentList(
            argumentList: PyArgumentList?,
            sectionName: String,
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