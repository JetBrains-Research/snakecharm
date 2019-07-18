package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkLambdaRuleParamsInspection : SnakemakeInspection() {
    private val wildcardsLambdaParameterName = "wildcards"
    private val allowedInParams = listOf(
            wildcardsLambdaParameterName,
            SnakemakeNames.SECTION_INPUT,
            SnakemakeNames.SECTION_OUTPUT,
            SnakemakeNames.SECTION_RESOURCES,
            SnakemakeNames.SECTION_THREADS
    )

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
                            if (pyParameter.name != wildcardsLambdaParameterName) {
                                registerProblem(
                                        pyParameter,
                                        SnakemakeBundle.message(
                                                "INSP.NAME.only.these.parameters.in.section",
                                                wildcardsLambdaParameterName,
                                                SnakemakeNames.SECTION_INPUT
                                        )
                                )
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
                            if (index == 0 && pyParameter.name != wildcardsLambdaParameterName) {
                                registerProblem(
                                        pyParameter,
                                        SnakemakeBundle.message("INSP.NAME.wildcards.first.argument")
                                )
                            }
                            if (index >= allowedInParams.size) {
                                registerProblem(
                                        pyParameter,
                                        SnakemakeBundle.message(
                                                "INSP.NAME.only.n.parameters.in.section",
                                                allowedInParams.size,
                                                SnakemakeNames.SECTION_PARAMS
                                        )
                                )
                            }
                            if (pyParameter.name !in allowedInParams) {
                                registerProblem(
                                        pyParameter,
                                        SnakemakeBundle.message(
                                                "INSP.NAME.only.these.parameters.in.section",
                                                allowedInParams.joinToString("/"),
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