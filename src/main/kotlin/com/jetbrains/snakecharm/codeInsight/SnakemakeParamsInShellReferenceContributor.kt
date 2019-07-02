package com.jetbrains.snakecharm.codeInsight

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.lang.psi.SMKParamsReference
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement
import java.util.regex.Pattern

class SnakemakeParamsInShellReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns
                        .psiElement(PyStringLiteralExpression::class.java)
                        .withParent(PyArgumentList::class.java),
                object : PsiReferenceProvider() {
                    private val identifierRegex = "[_a-zA-Z]\\w*"
                    private val paramsPattern = Pattern.compile("\\{params\\.($identifierRegex)")

                    override fun getReferencesByElement(
                            element: PsiElement,
                            context: ProcessingContext
                    ): Array<PsiReference> {
                        if (element.containingFile.language != SnakemakeLanguageDialect) {
                            return emptyArray()
                        }

                        val paramReferences = mutableListOf<PsiReference>()

                        val isShellCommand = element
                                .parentOfType<SMKRuleParameterListStatement>()
                                ?.section
                                ?.textMatches(SMKRuleParameterListStatement.SHELL) ?: return emptyArray()
                        if (element is PyStringLiteralExpression && isShellCommand) {

                            val paramsMatcher = paramsPattern.matcher(element.text)

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