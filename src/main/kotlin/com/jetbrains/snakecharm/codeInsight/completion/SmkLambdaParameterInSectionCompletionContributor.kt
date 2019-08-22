package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.TailTypeDecorator
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyCallExpression
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
            .andNot(PlatformPatterns.psiElement().inside(PyCallExpression::class.java)) // don't target lambda invocations
            .inside(SmkRuleOrCheckpointArgsSection::class.java)!!

    override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
    ) {
        val element = parameters.position
        val section = PsiTreeUtil.getParentOfType(element, SmkRuleOrCheckpointArgsSection::class.java) ?: return
        when (section.name) {
            SnakemakeNames.SECTION_INPUT, SnakemakeNames.SECTION_GROUP -> {
                result.addElement(
                        TailTypeDecorator.withTail(
                                LookupElementBuilder.create(SnakemakeNames.SMK_VARS_WILDCARDS)
                                        .withIcon(PlatformIcons.PARAMETER_ICON),
                                ColonAndWhiteSpaceTail
                        )
                )
            }
            SnakemakeNames.SECTION_PARAMS ->
                addCompletionResultsForSection(element, SmkLambdaRuleParamsInspection.ALLOWED_IN_PARAMS, result)
            SnakemakeNames.SECTION_THREADS ->
                addCompletionResultsForSection(element, SmkLambdaRuleParamsInspection.ALLOWED_IN_THREADS, result)
            SnakemakeNames.SECTION_RESOURCES ->
                addCompletionResultsForSection(element, SmkLambdaRuleParamsInspection.ALLOWED_IN_RESOURCES, result)
        }
    }

    private fun addCompletionResultsForSection(
            element: PsiElement,
            allowedVariants: Array<String>,
            result: CompletionResultSet
    ) {
        val lambdaExpression = PsiTreeUtil.getParentOfType(element, PyLambdaExpression::class.java)!!
        val presentParameters = lambdaExpression.parameterList.parameters.map { it.name }
        if (SnakemakeNames.SMK_VARS_WILDCARDS !in presentParameters) {
            result.addElement(
                    PrioritizedLookupElement.withPriority(
                            LookupElementBuilder
                                    .create(SnakemakeNames.SMK_VARS_WILDCARDS)
                                    .withIcon(PlatformIcons.PARAMETER_ICON)
                            , SmkCompletionUtil.WILDCARDS_LAMBDA_PARAMETER_PRIORITY

                    )
            )
        }

        val variants = allowedVariants
                .filterNot { it == SnakemakeNames.SMK_VARS_WILDCARDS }
                .filterNot { it in presentParameters }
        if (variants.size == 1) {
            result.addElement(TailTypeDecorator.withTail(
                    LookupElementBuilder.create(variants.first()).withIcon(PlatformIcons.PARAMETER_ICON),
                    ColonAndWhiteSpaceTail
            ))
        } else {
            result.addAllElements(variants
                    .map { LookupElementBuilder.create(it).withIcon(PlatformIcons.PARAMETER_ICON) })
        }
    }
}