package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.lang.Language
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.QualifiedName
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyPsiFacade
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.resolve.CompletionVariantsProcessor
import com.jetbrains.python.psi.resolve.PyResolveUtil

class ExpandCompletionContributor : CompletionContributor() {
    companion object {
        private val REFERENCE = psiElement(PyReferenceExpression::class.java)

        private val EXPAND_CAPTURE = psiElement()
                .withParent(REFERENCE)
        // TODO how to exclude calling the function as an object method?


        class SnakemakeExpandCompletionProvider : CompletionProvider<CompletionParameters>() {

            private fun resolveToFunction(file: PsiFile,
                                          element: PsiElement,
                                          functionName: String,
                                          dottedString: String): PyFunction? {
                val facade = PyPsiFacade.getInstance(file.project)
                val resolveContext = facade.createResolveContextFromFoothold(element).copyWithMembers()
                val qualifiedName = QualifiedName.fromDottedString(dottedString)
                val resolve = facade.resolveQualifiedName(qualifiedName, resolveContext)
                // TODO or are we satisfied with just (resolve[0] as PyFile)?
                resolve.map {
                    val function = (it as PyFile).findTopLevelFunction(functionName)
                    if (function != null) {
                        return function
                    }
                }
                return null
            }

            override fun addCompletions(parameters: CompletionParameters,
                                        context: ProcessingContext,
                                        result: CompletionResultSet) {
                if (parameters.originalFile.language != Language.findLanguageByID("Snakemake")) {
                    return
                }

                val ret = mutableListOf<LookupElement>()
                val ref = parameters.originalFile.findReferenceAt(parameters.offset - 1) ?: return
                val element = ref.element

                val scopeOwner = resolveToFunction(parameters.originalFile,
                        element, "expand", "snakemake.io")

                if (scopeOwner != null) {
                    val processor = CompletionVariantsProcessor(element)
                    PyResolveUtil.scopeCrawlUp(processor, scopeOwner, null, null)
                    ret.addAll(processor.resultList)
                }
                result.addAllElements(ret)
            }
        }
    }

    init {
        extend(CompletionType.BASIC, EXPAND_CAPTURE, SnakemakeExpandCompletionProvider())
    }
}