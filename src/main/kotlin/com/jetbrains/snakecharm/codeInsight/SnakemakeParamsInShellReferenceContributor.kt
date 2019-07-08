package com.jetbrains.snakecharm.codeInsight

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.codeInsight.completion.SMKKeywordCompletionContributor
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SMKParamsReference
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement
import java.util.regex.Pattern

class SnakemakeParamsInShellReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns
                        .psiElement(PyStringLiteralExpression::class.java)
                        .inFile(SMKKeywordCompletionContributor.IN_SNAKEMAKE)
                        .inside(SMKRuleParameterListStatement::class.java),
                object : PsiReferenceProvider() {
                    private val paramsPattern = Pattern.compile("\\{params\\.([_a-zA-Z]\\w*)")

                    override fun getReferencesByElement(
                            element: PsiElement,
                            context: ProcessingContext
                    ): Array<PsiReference> {
                        val paramReferences = mutableListOf<PsiReference>()
                        val paramsMatcher = paramsPattern.matcher(element.text)

                        val isShellCommand = PsiTreeUtil
                                .getParentOfType(element, SMKRuleParameterListStatement::class.java)!!
                                .section.textMatches(SnakemakeNames.SECTION_SHELL)
                        if (isShellCommand) {
                            while (paramsMatcher.find()) {
                                paramReferences.add(SMKParamsReference(element,
                                        TextRange(paramsMatcher.start(1), paramsMatcher.end(1))))
                            }
                        }

                        return paramReferences.toTypedArray()
                    }
        })
    }
}