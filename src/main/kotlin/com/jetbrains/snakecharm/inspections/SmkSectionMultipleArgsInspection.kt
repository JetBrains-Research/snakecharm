package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeApiService
import com.jetbrains.snakecharm.framework.SmkSupportProjectSettings
import com.jetbrains.snakecharm.framework.snakemakeAPIAnnotations.SmkApiAnnotationParsingContextType
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.*

class SmkSectionMultipleArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {
        val apiService = SnakemakeApiService.getInstance(holder.project)

        override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
            processSubSection(st, SnakemakeNames.SUBWORKFLOW_KEYWORD)
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            val contextKeyword = st.getParentRuleOrCheckPoint().sectionKeyword
            processSubSection(st, contextKeyword)
        }

        private fun processSubSection(st: SmkArgsSection, contextKeyword: String?) {
            val keyword = st.sectionKeyword
            if (keyword != null && contextKeyword != null && apiService.isSubsectionSingleArgumentOnly(
                    keyword,
                    contextKeyword
                )
            ) {
                checkArgumentList(st.argumentList, keyword)
            }
        }

        override fun visitSmkWorkflowArgsSection(st: SmkWorkflowArgsSection) {
            val keyword = st.sectionKeyword
            val contextType = SmkApiAnnotationParsingContextType.TOP_LEVEL.typeStr
            if (keyword != null && apiService.isSubsectionSingleArgumentOnly(keyword, contextType)) {
                checkArgumentList(st.argumentList, keyword)
            }
        }

        override fun visitSmkModuleArgsSection(st: SmkModuleArgsSection) {
            processSubSection(st, SnakemakeNames.MODULE_KEYWORD)
        }

        private fun checkArgumentList(
            argumentList: PyArgumentList?,
            sectionName: String,
        ) {
            val args = argumentList?.arguments ?: emptyArray()
            if (args.size > 1) {
                val settings = SmkSupportProjectSettings.getInstance(argumentList!!.project)
                val currentVersionString = settings.snakemakeLanguageVersion

                args.forEachIndexed { i, arg ->
                    if (i > 0) {
                        registerProblem(
                            arg,
                            SnakemakeBundle.message(
                                "INSP.NAME.section.multiple.args.in.lang.level.message", sectionName,
                                currentVersionString ?: "Unknown"
                            )
                        )
                    }
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.section.multiple.args")
}