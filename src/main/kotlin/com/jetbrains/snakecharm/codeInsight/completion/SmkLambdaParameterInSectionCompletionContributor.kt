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
import com.jetbrains.snakecharm.codeInsight.SnakemakeApiService
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SmkLambdaParameterInSectionCompletionContributor : CompletionContributor() {
    init {
        extend(
                CompletionType.BASIC,
                SmkLambdaParameterInSectionCompletionProvider.CAPTURE,
                SmkLambdaParameterInSectionCompletionProvider
        )
    }
}

object SmkLambdaParameterInSectionCompletionProvider : CompletionProvider<CompletionParameters>() {
    val CAPTURE = PlatformPatterns.psiElement(PyTokenTypes.IDENTIFIER)
            .inFile(SmkCompletionContributorPattern.IN_SNAKEMAKE)
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

        @Suppress("MoveVariableDeclarationIntoWhen")
        val sectionName = section.name
        when (sectionName) {
            SnakemakeNames.SECTION_INPUT, SnakemakeNames.SECTION_GROUP -> {
                result.addElement(
                        TailTypeDecorator.withTail(
                                LookupElementBuilder.create(SnakemakeNames.SMK_VARS_WILDCARDS)
                                        .withIcon(PlatformIcons.PARAMETER_ICON),
                                ColonAndWhiteSpaceTail
                        )
                )
            }
            else -> {
                val apiService = SnakemakeApiService.getInstance(element.project)
                val context = section.getParentRuleOrCheckPoint().sectionKeyword
                val args = apiService.getLambdaArgsForSubsection(sectionName, context)
                if (args.isNotEmpty()) {
                    addCompletionResultsForSection(element, args, result)
                }
            }
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