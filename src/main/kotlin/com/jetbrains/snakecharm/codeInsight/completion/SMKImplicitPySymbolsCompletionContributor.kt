package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.resolve.CompletionVariantsProcessor
import com.jetbrains.snakecharm.codeInsight.ImplicitPySymbolsCache
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect

class SMKImplicitPySymbolsCompletionContributor : CompletionContributor() {
    companion object {
        private val REFERENCE = PlatformPatterns.psiElement(PyReferenceExpression::class.java)

        private val REF_CAPTURE = PlatformPatterns.psiElement()
                .inside(REFERENCE)
                .inFile(PlatformPatterns.psiFile().withLanguage(SnakemakeLanguageDialect))
                .with(object : PatternCondition<PsiElement>("isFirstChild") {
                    override fun accepts(element: PsiElement, context: ProcessingContext): Boolean {
                        // check that this element is "first" in parent PyReferenceExpression, e.g. that
                        // we don't have some prefix with '.', e.g. element is 'exp<caret>', not 'foo.exp<caret>'
                        return !(element.parentOfType<PyReferenceExpression>()?.isQualified ?: true)
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
            val processor = object : CompletionVariantsProcessor(contextElement) {
                public override fun addElement(name: String, element: PsiElement) {
                    super.addElement(name, element)
                }
            }
            ImplicitPySymbolsCache.instance(module).all().forEach { (name, psi) ->
                // processor.execute(it, ResolveState.initial())
                processor.addElement(name, psi)
            }

            result.addAllElements(processor.resultList)
        }
    }
}