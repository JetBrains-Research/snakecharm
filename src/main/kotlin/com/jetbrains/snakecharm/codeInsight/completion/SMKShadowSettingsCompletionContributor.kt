package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.parentOfType
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement

class SMKShadowSettingsCompletionContributor : CompletionContributor() {
    init {
        extend(
                CompletionType.BASIC,
                ShadowSectionSettingsProvider.CAPTURE,
                ShadowSectionSettingsProvider
        )
    }
}

object ShadowSectionSettingsProvider : CompletionProvider<CompletionParameters>() {
    val CAPTURE = PlatformPatterns.psiElement()
            .inFile(SMKKeywordCompletionContributor.IN_SNAKEMAKE)
            .inside(SMKKeywordCompletionContributor.IN_RULE)
            .inside(SMKKeywordCompletionContributor.IN_RULE_SECTION)
            .inside(PyStringLiteralExpression::class.java)!!

    val SHADOW_SETTINGS = listOf("shallow", "full", "minimal")

    override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
    ) {
        val parentListStatement = parameters.position.parentOfType<SMKRuleParameterListStatement>()
        if (parentListStatement?.name == SMKRuleParameterListStatement.SHADOW) {
            SHADOW_SETTINGS.forEach {
                result.addElement(LookupElementBuilder.create(it).withIcon(PlatformIcons.PARAMETER_ICON))
            }
        }

    }
}