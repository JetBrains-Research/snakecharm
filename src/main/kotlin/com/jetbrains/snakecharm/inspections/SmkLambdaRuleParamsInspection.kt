package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.inspections.quickfix.RenameParameterQuickFix
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeApi
import com.jetbrains.snakecharm.codeInsight.SnakemakeApiService
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkLambdaRuleParamsInspection : SnakemakeInspection() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ) = object : SnakemakeInspectionVisitor(holder, getContext(session)) {
        val apiService = SnakemakeApiService.getInstance(holder.project)

        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            val lambdas = st.argumentList?.arguments?.filterIsInstance<PyLambdaExpression>() ?: emptyList()
            val allLambdas = lambdas +
                    (st.argumentList?.arguments
                        ?.filterIsInstance<PyKeywordArgument>()
                        ?.map { it.valueExpression }
                        ?.filterIsInstance<PyLambdaExpression>()
                        ?: emptyList())
            // see https://github.com/JetBrains-Research/snakecharm/issues/187
            // for more info on which sections are allowed to use callables and why

            val sectionKeyword = st.sectionKeyword
            if (sectionKeyword == null) {
                return
            }

            val context = st.getParentRuleOrCheckPoint().sectionKeyword
            val allowedArgs = apiService.getLambdaArgsForSubsection(sectionKeyword, context)
            if (allowedArgs.isNotEmpty()) {
                registerParamsProblemsForLambdasWithWildcards(
                    allLambdas,
                    sectionKeyword,
                    *allowedArgs
                )
            } else {
                allLambdas.forEach {
                    registerProblem(
                        it,
                        SnakemakeBundle.message(
                            "INSP.NAME.functions.not.allowed.in.section",
                            st.sectionKeyword!!
                        )
                    )
                }
            }
        }

        private fun registerParamsProblemsForLambdasWithWildcards(
            lambdas: List<PyLambdaExpression>,
            sectionName: String,
            vararg optionalParameters: String,
        ) {
            lambdas.forEach { lambda ->
                lambda.parameterList.parameters.forEachIndexed { index, pyParameter ->
                    if (index == 0 && pyParameter.name != SnakemakeApi.SMK_VARS_WILDCARDS) {
                        if (pyParameter.name in optionalParameters) {
                            registerProblem(
                                pyParameter,
                                SnakemakeBundle.message(
                                    "INSP.NAME.non.wildcards.param.first.parameter",
                                    pyParameter.name!!,
                                    sectionName
                                )
                            )
                        } else {
                            registerProblem(
                                pyParameter,
                                SnakemakeBundle.message("INSP.NAME.wildcards.first.parameter.preferable"),
                                ProblemHighlightType.WEAK_WARNING,
                                null,
                                RenameParameterQuickFix(SnakemakeApi.SMK_VARS_WILDCARDS)
                            )
                        }
                    }
                    if (index != 0 && pyParameter.name == SnakemakeApi.SMK_VARS_WILDCARDS) {
                        registerProblem(
                            pyParameter,
                            SnakemakeBundle.message("INSP.NAME.wildcards.first.parameter")
                        )
                    }
                    if (index >= optionalParameters.size) {
                        registerProblem(
                            pyParameter,
                            SnakemakeBundle.message(
                                "INSP.NAME.only.n.parameters.in.section",
                                optionalParameters.size,
                                sectionName
                            )
                        )
                    }
                    if (index != 0 && pyParameter.name !in optionalParameters) {
                        registerProblem(
                            pyParameter,
                            SnakemakeBundle.message(
                                "INSP.NAME.only.these.parameters.in.section",
                                optionalParameters.joinToString("/"),
                                sectionName
                            )
                        )
                    }
                }
            }
        }
    }
}