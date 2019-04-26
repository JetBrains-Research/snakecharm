package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.Language
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.util.ProcessingContext
import com.jetbrains.python.codeInsight.completion.addFunctionToResult
import com.jetbrains.python.codeInsight.completion.createLookupElementBuilder
import com.jetbrains.python.codeInsight.completion.getFile
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect

class ExpandCompletionContributor : CompletionContributor() {
    companion object {
        private val EXPAND_CAPTURE = PlatformPatterns
                .psiElement()
                .withParent(PyReferenceExpression::class.java)

        class SnakemakeExpandCompletionProvider(
                private val keywords: Array<String> = arrayOf("expand")
        ) : CompletionProvider<CompletionParameters>() {

            override fun addCompletions(parameters: CompletionParameters,
                                        context: ProcessingContext,
                                        result: CompletionResultSet) {
                if (parameters.originalFile.language == Language.findLanguageByID("Python")) {
                    return
                }

                result.addAllElements(keywords.map {
                    LookupElementBuilder
                            .create(it)
                            .withInsertHandler(ParenthesesInsertHandler.WITH_PARAMETERS)
                            .withTailText("(args, wildcards)")
                            .withTypeText("snakemake.io")
                })
            }
        }
    }

    init {
        extend(CompletionType.BASIC, EXPAND_CAPTURE, SnakemakeExpandCompletionProvider())
    }

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        super.fillCompletionVariants(parameters, result)
    }
}