package com.jetbrains.snakecharm.codeInsight

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.codeInsight.completion.SMKKeywordCompletionContributor
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SMKParamsReference
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement
import com.jetbrains.snakecharm.lang.psi.SMKRuleRunParameter
import java.util.regex.Pattern

class SnakemakeParamsInShellReferenceContributor : PsiReferenceContributor() {
    private val insideRuleSection = PlatformPatterns.psiElement()
            .inFile(SMKKeywordCompletionContributor.IN_SNAKEMAKE)
            .inside(SMKRuleParameterListStatement::class.java)
    private val insideCallExpressionInRuleRunParameter = PlatformPatterns.psiElement()
            .inFile(SMKKeywordCompletionContributor.IN_SNAKEMAKE)
            .inside(PyCallExpression::class.java)
            .withAncestor(6, PlatformPatterns.psiElement(SMKRuleRunParameter::class.java))


    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
                PlatformPatterns
                        .psiElement(PyStringLiteralExpression::class.java)
                        .andOr(insideRuleSection, insideCallExpressionInRuleRunParameter),
                object : PsiReferenceProvider() {
                    private val paramsPattern = Pattern.compile("\\{params\\.([_a-zA-Z]\\w*)")

                    override fun getReferencesByElement(
                            element: PsiElement,
                            context: ProcessingContext
                    ): Array<PsiReference> {
                        val paramReferences = mutableListOf<PsiReference>()
                        val paramsMatcher = paramsPattern.matcher(element.text)

                        val isShellCommand = PsiTreeUtil
                                .getParentOfType(element, SMKRuleParameterListStatement::class.java)
                                ?.section?.textMatches(SnakemakeNames.SECTION_SHELL) == true
                        if (isShellCommand) {
                            while (paramsMatcher.find()) {
                                paramReferences.add(SMKParamsReference(element  as PyStringLiteralExpression,
                                        TextRange(paramsMatcher.start(1), paramsMatcher.end(1))))
                            }
                        } else {
                            val isShellCallExpression =
                                    PsiTreeUtil.getParentOfType(element, PyCallExpression::class.java)!!
                                            .callee?.name == SnakemakeNames.SECTION_SHELL
                            if (isShellCallExpression) {
                                while (paramsMatcher.find()) {
                                    paramReferences.add(SMKParamsReference(element as PyStringLiteralExpression,
                                            TextRange(paramsMatcher.start(1), paramsMatcher.end(1))))
                                }
                            }
                        }

                        return paramReferences.toTypedArray()
                    }
        })
    }
}