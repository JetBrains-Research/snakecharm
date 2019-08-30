package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.resolve.CompletionVariantsProcessor
import com.jetbrains.snakecharm.codeInsight.ImplicitPySymbolsProvider
import com.jetbrains.snakecharm.codeInsight.SmkCodeInsightScope
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

class SMKImplicitPySymbolsCompletionContributor : CompletionContributor() {
    companion object {
        val IN_PY_REF = psiElement().inside(PyReferenceExpression::class.java)

        private val REF_CAPTURE = psiElement()
                .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
                .and(IN_PY_REF)
                .with(object : PatternCondition<PsiElement>("isFirstChild") {
                    override fun accepts(element: PsiElement, context: ProcessingContext): Boolean {
                        // check that this element is "first" in parent PyReferenceExpression, e.g. that
                        // we don't have some prefix with '.', e.g. element is 'exp<caret>', not 'foo.exp<caret>'
                        
                        val refExpression = PsiTreeUtil.getParentOfType(element, PyReferenceExpression::class.java)
                        return !(refExpression?.isQualified ?: true)
                    }
                })
    }

    init {
        extend(CompletionType.BASIC, REF_CAPTURE, SMKImplicitPySymbolsCompletionProvider())
    }
}

class SMKImplicitPySymbolsCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(parameters: CompletionParameters,
                                context: ProcessingContext,
                                result: CompletionResultSet) {

        val contextElement = parameters.position

        val module = ModuleUtilCore.findModuleForPsiElement(parameters.originalFile)
        if (module != null) {
            val processor = SmkCompletionVariantsProcessor(contextElement)
            val contextScope = SmkCodeInsightScope[contextElement]
            val cache = ImplicitPySymbolsProvider.instance(module).cache

            SmkCodeInsightScope.values().asSequence()
                    .filter { symbolScope -> contextScope.includes(symbolScope) }
                    .flatMap { symbolScope -> cache[symbolScope].asSequence() }
                    .forEach { symbol ->
                        // processor.execute(it, ResolveState.initial())
                        processor.addElement(symbol.identifier, symbol.psiDeclaration)
                    }

            if (contextScope == SmkCodeInsightScope.RULELIKE_RUN_SECTION) {
                val ruleOrCheckpoint = PsiTreeUtil.getParentOfType(contextElement, SmkRuleOrCheckpoint::class.java)!!
                val threadsSection = ruleOrCheckpoint.statementList.statements.asSequence()
                        .filterIsInstance<SmkRuleOrCheckpointArgsSection>()
                        .filter { it.name == SnakemakeNames.SECTION_THREADS }.firstOrNull()
                processor.addElement("threads", threadsSection ?: ruleOrCheckpoint)
            }
            result.addAllElements(processor.resultList)
        }
    }
}

/**
 * Just makes `addElement` public. PyCharm API required here
 */
class SmkCompletionVariantsProcessor(context: PsiElement) : CompletionVariantsProcessor(context) {
    public override fun addElement(name: String, element: PsiElement) = super.addElement(name, element)
}