package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SECTIONS_WHERE_KEYWORD_ARGS_PROHIBITED
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.WORKFLOWS_WHERE_KEYWORD_ARGS_PROHIBITED
import com.jetbrains.snakecharm.lang.psi.*

class SmkSectionUnexpectedKeywordArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {

        override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
            checkArgumentList(st.argumentList, st)
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            checkArgumentList(st, SECTIONS_WHERE_KEYWORD_ARGS_PROHIBITED)
        }

        override fun visitSmkWorkflowArgsSection(st: SmkWorkflowArgsSection) {
            checkArgumentList(st, WORKFLOWS_WHERE_KEYWORD_ARGS_PROHIBITED)
        }

        private fun checkArgumentList(
            st: SmkArgsSection,
            sectionKeywords: Set<String>,
        ) {
            val keyword = st.sectionKeyword
            if (keyword != null && keyword in sectionKeywords) {
                checkArgumentList(st.argumentList, st)
            }
        }

        override fun visitSmkModuleArgsSection(st: SmkModuleArgsSection) {
            checkArgumentList(st.argumentList, st)
        }

        private fun checkArgumentList(
            argumentList: PyArgumentList?,
            section: SmkArgsSection
        ) {
            val args = argumentList?.arguments ?: emptyArray()
            args.forEach { arg ->
                if (arg is PyKeywordArgument) {
                    registerProblem(
                        arg,
                        SnakemakeBundle.message(
                            "INSP.NAME.section.unexpected.keyword.args.message",
                            section.sectionKeyword!!
                        )
                    )
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.section.unexpected.keyword.args")
}
