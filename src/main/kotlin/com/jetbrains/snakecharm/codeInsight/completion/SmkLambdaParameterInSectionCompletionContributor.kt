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
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.ALLOWED_LAMBDA_ARGS
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

        @Suppress("MoveVariableDeclarationIntoWhen")
        val sectionName = section.name
        when (sectionName) {
            SnakemakeNames.SECTION_INPUT, SnakemakeNames.SECTION_GROUP -> {
                result.addElement(
                        TailTypeDecorator.withTail(
                                LookupElementBuilder.create(SnakemakeAPI.SMK_VARS_WILDCARDS)
                                        .withIcon(PlatformIcons.PARAMETER_ICON),
                                ColonAndWhiteSpaceTail
                        )
                )
            }
            else -> {
                val args = ALLOWED_LAMBDA_ARGS[sectionName]
                if (args != null) {
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
        if (SnakemakeAPI.SMK_VARS_WILDCARDS !in presentParameters) {
            result.addElement(
                    PrioritizedLookupElement.withPriority(
                            LookupElementBuilder
                                    .create(SnakemakeAPI.SMK_VARS_WILDCARDS)
                                    .withIcon(PlatformIcons.PARAMETER_ICON)
                            , SmkCompletionUtil.WILDCARDS_LAMBDA_PARAMETER_PRIORITY

                    )
            )
        }

        val variants = allowedVariants
                .filterNot { it == SnakemakeAPI.SMK_VARS_WILDCARDS }
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