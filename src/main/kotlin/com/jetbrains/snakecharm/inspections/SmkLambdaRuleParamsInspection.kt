package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.inspections.quickfix.PyRenameElementQuickFix
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkLambdaRuleParamsInspection : SnakemakeInspection() {
    companion object {
        const val WILDCARDS_LAMBDA_PARAMETER = "wildcards"
        val ALLOWED_IN_PARAMS = listOf(
                WILDCARDS_LAMBDA_PARAMETER,
                SnakemakeNames.SECTION_INPUT,
                SnakemakeNames.SECTION_OUTPUT,
                SnakemakeNames.SECTION_RESOURCES,
                SnakemakeNames.SECTION_THREADS
        )
    }

    override fun buildVisitor(
            holder: ProblemsHolder,
            isOnTheFly: Boolean,
            session: LocalInspectionToolSession
    ) = object : SnakemakeInspectionVisitor(holder, session) {
        override fun visitSmkRuleOrCheckpointArgsSection(st: SmkRuleOrCheckpointArgsSection) {
            val lambdas = st.argumentList?.arguments?.filterIsInstance<PyLambdaExpression>() ?: emptyList()

            when (st.sectionKeyword) {
                SnakemakeNames.SECTION_INPUT -> {
                    lambdas.forEach { lambda ->
                        lambda.parameterList.parameters.forEachIndexed { index, pyParameter ->
                            if (index > 0) {
                                registerProblem(
                                        pyParameter,
                                        SnakemakeBundle.message(
                                                "INSP.NAME.only.n.parameters.in.section",
                                                1,
                                                SnakemakeNames.SECTION_INPUT
                                        )
                                )
                            }
                            if (pyParameter.name != WILDCARDS_LAMBDA_PARAMETER) {
                                if (index == 0) {
                                    registerProblem(
                                            pyParameter,
                                            SnakemakeBundle.message(
                                                    "INSP.NAME.wildcards.first.parameter.preferable"
                                            ),
                                            ProblemHighlightType.WEAK_WARNING,
                                            null,
                                            PyRenameElementQuickFix(pyParameter)
                                    )
                                } else {
                                    registerProblem(
                                            pyParameter,
                                            SnakemakeBundle.message(
                                                    "INSP.NAME.only.these.parameters.in.section",
                                                    WILDCARDS_LAMBDA_PARAMETER,
                                                    SnakemakeNames.SECTION_INPUT
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
                SnakemakeNames.SECTION_PARAMS -> {
                    val allLambdas = lambdas +
                            (st.argumentList?.arguments
                                    ?.filterIsInstance<PyKeywordArgument>()
                                    ?.map { it.valueExpression }
                                    ?.filterIsInstance<PyLambdaExpression>()
                                    ?: emptyList())

                    allLambdas.forEach { lambda ->

                        lambda.parameterList.parameters.forEachIndexed { index, pyParameter ->
                            if (index == 0 && pyParameter.name != WILDCARDS_LAMBDA_PARAMETER) {
                                if (pyParameter.name in ALLOWED_IN_PARAMS) {
                                    registerProblem(
                                            pyParameter,
                                            SnakemakeBundle.message(
                                                    "INSP.NAME.non.wildcards.param.first.parameter",
                                                    pyParameter.name!!,
                                                    SnakemakeNames.SECTION_PARAMS
                                            )
                                    )
                                } else {
                                    registerProblem(
                                            pyParameter,
                                            SnakemakeBundle.message("INSP.NAME.wildcards.first.parameter.preferable"),
                                            ProblemHighlightType.WEAK_WARNING,
                                            null,
                                            PyRenameElementQuickFix(pyParameter)
                                    )
                                }
                            }
                            if (index != 0 && pyParameter.name == WILDCARDS_LAMBDA_PARAMETER) {
                                registerProblem(
                                        pyParameter,
                                        SnakemakeBundle.message("INSP.NAME.wildcards.first.parameter")
                                )
                            }
                            if (index >= ALLOWED_IN_PARAMS.size) {
                                registerProblem(
                                        pyParameter,
                                        SnakemakeBundle.message(
                                                "INSP.NAME.only.n.parameters.in.section",
                                                ALLOWED_IN_PARAMS.size,
                                                SnakemakeNames.SECTION_PARAMS
                                        )
                                )
                            }
                            if (index != 0 && pyParameter.name !in ALLOWED_IN_PARAMS) {
                                registerProblem(
                                        pyParameter,
                                        SnakemakeBundle.message(
                                                "INSP.NAME.only.these.parameters.in.section",
                                                ALLOWED_IN_PARAMS.joinToString("/"),
                                                SnakemakeNames.SECTION_PARAMS
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}