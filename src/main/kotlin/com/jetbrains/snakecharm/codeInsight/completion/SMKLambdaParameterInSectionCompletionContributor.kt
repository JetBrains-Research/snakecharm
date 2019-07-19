package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.TailTypeDecorator
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.python.psi.PyParameterList
import com.jetbrains.snakecharm.inspections.SmkLambdaRuleParamsInspection
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SMKLambdaParameterInSectionCompletionContributor : CompletionContributor() {
    init {
        extend(
                CompletionType.BASIC,
                SMKLambdaParameterInSectionCompletionProvider.CAPTURE,
                SMKLambdaParameterInSectionCompletionProvider
        )
    }
}

object SMKLambdaParameterInSectionCompletionProvider : CompletionProvider<CompletionParameters>() {
    val CAPTURE = PlatformPatterns.psiElement(PyTokenTypes.IDENTIFIER)
            .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
            .inside(PyParameterList::class.java)
            .inside(PyLambdaExpression::class.java)
            .inside(SmkRuleOrCheckpointArgsSection::class.java)!!

    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val element = parameters.position
        val section = element.parentOfType(SmkRuleOrCheckpointArgsSection::class) ?: return
        when (section.name) {
            SnakemakeNames.SECTION_INPUT -> {
                result.addElement(
                        TailTypeDecorator.withTail(
                                LookupElementBuilder.create(SmkLambdaRuleParamsInspection.WILDCARDS_LAMBDA_PARAMETER),
                                ColonAndWhiteSpaceTail
                        )
                )
            }
            SnakemakeNames.SECTION_PARAMS -> {
                val lambdaExpression = element.parentOfType<PyLambdaExpression>()!!
                val presentParameters = lambdaExpression.parameterList.parameters.map { it.name }
                if (SmkLambdaRuleParamsInspection.WILDCARDS_LAMBDA_PARAMETER !in presentParameters) {
                    result.addElement(
                            PrioritizedLookupElement.withPriority(
                                    LookupElementBuilder
                                            .create(SmkLambdaRuleParamsInspection.WILDCARDS_LAMBDA_PARAMETER)
                                    , 0.1 // TODO what should be the actual value here? just need this to be prioritized over all other variants

                            )
                    )
                }
                result.addAllElements(
                        SmkLambdaRuleParamsInspection.ALLOWED_IN_PARAMS
                                .filterNot { it == SmkLambdaRuleParamsInspection.WILDCARDS_LAMBDA_PARAMETER }
                                .filterNot { it in presentParameters }
                                .map { LookupElementBuilder.create(it) }
                )
            }
        }
    }
}