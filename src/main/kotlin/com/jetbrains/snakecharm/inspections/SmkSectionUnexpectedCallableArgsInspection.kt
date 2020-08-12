package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.types.PyFunctionType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.ALLOWED_CALLABLE_ARGS
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflowArgsSection

class SmkSectionUnexpectedCallableArgsInspection : SnakemakeInspection() {
    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {

        override fun visitSmkSubworkflowArgsSection(st: SmkSubworkflowArgsSection) {
            if (st.sectionKeyword !in ALLOWED_CALLABLE_ARGS) {
                checkArgumentList(st.argumentList, st)
            }
        }

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            if (st.sectionKeyword !in ALLOWED_CALLABLE_ARGS) {
                checkArgumentList(st.argumentList, st)
            }
        }

        private fun checkArgumentList(
                argumentList: PyArgumentList?,
                section: SmkArgsSection
        ) {
            val args = argumentList?.arguments ?: emptyArray()
            args.forEach { arg ->
                if (arg is PyReferenceExpression) {
                    val childType = TypeEvalContext.codeAnalysis(section.project, section.containingFile).getType(arg)
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