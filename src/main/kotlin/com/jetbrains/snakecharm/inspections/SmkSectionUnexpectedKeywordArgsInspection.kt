package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPIProjectService
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.framework.snakemakeAPIAnnotations.SmkAPIAnnParsingContextType
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.*

class SmkSectionUnexpectedKeywordArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {
        val apiService = SnakemakeAPIProjectService.getInstance(holder.project)

        override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
            processSubSection(st, SnakemakeNames.SUBWORKFLOW_KEYWORD)
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            val contextKeyword = st.getParentRuleOrCheckPoint().sectionKeyword
            processSubSection(st, contextKeyword)
        }

        override fun visitSmkWorkflowArgsSection(st: SmkWorkflowArgsSection) {
            processSubSection(st, SmkAPIAnnParsingContextType.TOP_LEVEL.typeStr)
        }

        private fun processSubSection(st: SmkArgsSection, contextKeyword: String?) {
            val keyword = st.sectionKeyword
            if (keyword != null && contextKeyword != null && apiService.isSubsectionWithOnlyPositionalArguments(
                    keyword,
                    contextKeyword
                )
            ) {
                checkArgumentList(st.argumentList, st)
            }
        }

        override fun visitSmkModuleArgsSection(st: SmkModuleArgsSection) {
            processSubSection(st, SnakemakeNames.MODULE_KEYWORD)
        }

        private fun checkArgumentList(
            argumentList: PyArgumentList?,
            section: SmkArgsSection
        ) {
            val args = argumentList?.arguments ?: emptyArray()
            if (args.isEmpty()) {
                return
            }
            val settings = SmkSupportProjectSettings.getInstance(argumentList!!.project)
            val currentVersionString = settings.snakemakeLanguageVersion
            val sectionName = section.sectionKeyword!!
            args.forEach { arg ->
                if (arg is PyKeywordArgument) {
                    registerProblem(
                        arg,
                        SnakemakeBundle.message(
                            "INSP.NAME.section.unexpected.keyword.args.in.lang.level.message",
                            sectionName,
                            currentVersionString ?: "Unknown"
                        )
                    )
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.section.unexpected.keyword.args")
}
