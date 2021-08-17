package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.snakecharm.codeInsight.ImplicitPySymbolsProvider
import com.jetbrains.snakecharm.codeInsight.SmkCodeInsightScope
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionContributorUtils.Companion.checkTokenSequence

class SmkPepObjectCompletionContributor : CompletionContributor() {
    companion object {
        private val IN_PY_REF = PlatformPatterns.psiElement().inside(PyReferenceExpression::class.java)
        private val REF_CAPTURE = PlatformPatterns.psiElement()
            .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
            .and(IN_PY_REF)
            .with(object : PatternCondition<PsiElement>("isLastChild") {
                override fun accepts(element: PsiElement, context: ProcessingContext): Boolean {
                    val tokenList = listOf(PyTokenTypes.DOT, SnakemakeAPI.SMK_VARS_PEP)
                    return checkTokenSequence(tokenList, element)
                }
            })
    }

    init {
        extend(CompletionType.BASIC, REF_CAPTURE, SMKPepObjectCompletionProvider())
    }
}

class SMKPepObjectCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val contextElement = parameters.position
        val project = parameters.originalFile.project
        val cache = ImplicitPySymbolsProvider.instance(project).cache
        val processor = SmkCompletionVariantsProcessor(contextElement)
        cache[SmkCodeInsightScope.PEP_SECTION].forEach { symbol ->
            processor.addElement(symbol.identifier, symbol.psiDeclaration)
        }
        result.addAllElements(processor.resultList)
    }
}