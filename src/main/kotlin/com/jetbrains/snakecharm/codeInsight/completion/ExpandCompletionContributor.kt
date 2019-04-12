package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiIdentifier
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect

class ExpandCompletionContributor : CompletionContributor() {
    companion object {
        private val EXPAND_CAPTURE = PlatformPatterns
                .psiElement(PsiIdentifier::class.java)
                .withLanguage(SnakemakeLanguageDialect)
                .withParent(PyReferenceExpression::class.java)
                .withParent(PyCallExpression::class.java)

        class SnakemakeExpandCompletionProvider(
                private val keywords: Array<String> = arrayOf("expand")
        ) : CompletionProvider<CompletionParameters>() {

            override fun addCompletions(parameters: CompletionParameters,
                                        context: ProcessingContext,
                                        result: CompletionResultSet) {
                result.addAllElements(keywords.map { LookupElementBuilder.create(it) })
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