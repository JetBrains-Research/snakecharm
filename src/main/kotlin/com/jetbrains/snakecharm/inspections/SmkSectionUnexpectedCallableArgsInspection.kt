package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.types.PyFunctionType
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPIProjectService
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflowArgsSection

class SmkSectionUnexpectedCallableArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {
        val apiService = SnakemakeAPIProjectService.getInstance(holder.project)


        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            checkArgumentList(st.argumentList, st)
        }

        private fun checkArgumentList(
            argumentList: PyArgumentList?,
            section: SmkArgsSection,
        ) {
            if (apiService.getLambdaArgsFor(section.sectionKeyword) != null) {
                return
            }

            val args = argumentList?.arguments ?: emptyArray()
            args.forEach { arg ->
                if (arg is PyReferenceExpression && arg !is PyLambdaExpression) {
                    val childType = myTypeEvalContext.getType(arg)

                    if (childType is PyFunctionType) {
                        registerProblem(
                            arg,
                            SnakemakeBundle.message(
                                "INSP.NAME.section.unexpected.callable.args.message",
                                section.sectionKeyword!!
                            )
                        )
                    }
                }
            }
        }
    }

    override fun getDisplayName(): String = SnakemakeBundle.message("INSP.NAME.section.unexpected.callable.args")
}