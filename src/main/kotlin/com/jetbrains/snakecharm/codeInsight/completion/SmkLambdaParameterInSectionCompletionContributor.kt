package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.TailTypeDecorator
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyLambdaExpression
import com.jetbrains.python.psi.PyParameterList
import com.jetbrains.snakecharm.inspections.SmkLambdaRuleParamsInspection
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkLambdaParameterInSectionCompletionContributor : CompletionContributor() {
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
        val section = PsiTreeUtil.getParentOfType(element, SmkRuleOrCheckpointArgsSection::class.java) ?: return
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
                val lambdaExpression = PsiTreeUtil.getParentOfType(element, PyLambdaExpression::class.java)!!
                val presentParameters = lambdaExpression.parameterList.parameters.map { it.name }
                if (SmkLambdaRuleParamsInspection.WILDCARDS_LAMBDA_PARAMETER !in presentParameters) {
                    result.addElement(
                            PrioritizedLookupElement.withPriority(
                                    LookupElementBuilder
                                            .create(SmkLambdaRuleParamsInspection.WILDCARDS_LAMBDA_PARAMETER)
                                    , SmkCompletionUtil.WILDCARDS_LAMBDA_PARAMETER_PRIORITY

                            )
                    )
                }

                val variants = SmkLambdaRuleParamsInspection.ALLOWED_IN_PARAMS
                        .filterNot { it == SmkLambdaRuleParamsInspection.WILDCARDS_LAMBDA_PARAMETER }
                        .filterNot { it in presentParameters }
                if (variants.size == 1) {
                    result.addElement(TailTypeDecorator.withTail(
                            LookupElementBuilder.create(variants.first()),
                            ColonAndWhiteSpaceTail
                    ))
                } else {
                    result.addAllElements(variants.map { LookupElementBuilder.create(it) })
                }
            }
        }
    }
}