package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.inspections.quickfix.RenameParameterQuickFix
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkLambdaRuleParamsInspection : SnakemakeInspection() {
    companion object {
        const val WILDCARDS_LAMBDA_PARAMETER = "wildcards"
        private const val ATTEMPT_LAMBDA_PARAMETER = "attempt"
        val ALLOWED_IN_PARAMS = arrayOf(
                WILDCARDS_LAMBDA_PARAMETER,
                SnakemakeNames.SECTION_INPUT,
                SnakemakeNames.SECTION_OUTPUT,
                SnakemakeNames.SECTION_RESOURCES,
                SnakemakeNames.SECTION_THREADS
        )
        val ALLOWED_IN_RESOURCES = arrayOf(
                WILDCARDS_LAMBDA_PARAMETER,
                SnakemakeNames.SECTION_INPUT,
                SnakemakeNames.SECTION_THREADS,
                ATTEMPT_LAMBDA_PARAMETER
        )
        val ALLOWED_IN_THREADS = arrayOf(
                WILDCARDS_LAMBDA_PARAMETER,
                SnakemakeNames.SECTION_INPUT,
                ATTEMPT_LAMBDA_PARAMETER
        )
    }

    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            val lambdas = st.argumentList?.arguments?.filterIsInstance<PyLambdaExpression>() ?: emptyList()
            val allLambdas = lambdas +
                    (st.argumentList?.arguments
                            ?.filterIsInstance<PyKeywordArgument>()
                            ?.map { it.valueExpression }
                            ?.filterIsInstance<PyLambdaExpression>()
                            ?: emptyList())

            when (st.sectionKeyword) {
                SnakemakeNames.SECTION_INPUT, SnakemakeNames.SECTION_GROUP ->
                    registerParamsProblemsForLambdasWithWildcards(
                            allLambdas,
                            st.sectionKeyword!!,
                            WILDCARDS_LAMBDA_PARAMETER
                    )
                SnakemakeNames.SECTION_PARAMS ->
                    registerParamsProblemsForLambdasWithWildcards(
                            allLambdas,
                            SnakemakeNames.SECTION_PARAMS,
                            *ALLOWED_IN_PARAMS
                    )
                SnakemakeNames.SECTION_RESOURCES ->
                    registerParamsProblemsForLambdasWithWildcards(
                            allLambdas,
                            SnakemakeNames.SECTION_RESOURCES,
                            *ALLOWED_IN_RESOURCES
                    )
                SnakemakeNames.SECTION_THREADS ->
                    registerParamsProblemsForLambdasWithWildcards(
                            allLambdas,
                            SnakemakeNames.SECTION_THREADS,
                            *ALLOWED_IN_THREADS
                    )
                else -> {
                    if (st.sectionKeyword != null) {
                        allLambdas.forEach {
                            registerProblem(
                                    it,
                                    SnakemakeBundle.message(
                                            "INSP.NAME.callables.not.allowed.in.section",
                                            st.sectionKeyword!!
                                    )
                            )
                        }
                    }
                }
            }
        }

        private fun registerParamsProblemsForLambdasWithWildcards(
                lambdas: List<PyLambdaExpression>,
                sectionName: String,
                vararg optionalParameters: String
        ) {
            lambdas.forEach { lambda ->
                lambda.parameterList.parameters.forEachIndexed { index, pyParameter ->
                    if (index == 0 && pyParameter.name != WILDCARDS_LAMBDA_PARAMETER) {
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
                                    RenameParameterQuickFix(WILDCARDS_LAMBDA_PARAMETER)
                            )
                        }
                    }
                    if (index != 0 && pyParameter.name == WILDCARDS_LAMBDA_PARAMETER) {
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