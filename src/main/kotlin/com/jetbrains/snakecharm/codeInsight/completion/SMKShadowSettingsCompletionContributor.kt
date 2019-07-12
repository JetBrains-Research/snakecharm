package com.jetbrains.snakecharm.codeInsight.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpointArgsSection

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
            .inside(SmkRuleOrCheckpointArgsSection::class.java)
            .inside(PyStringLiteralExpression::class.java)!!

    val SHADOW_SETTINGS = listOf("shallow", "full", "minimal")

    override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet
    ) {
        val parentListStatement = PsiTreeUtil.getParentOfType(parameters.position, SmkRuleOrCheckpointArgsSection::class.java)!!
        if (parentListStatement.name == SnakemakeNames.SECTION_SHADOW) {
            SHADOW_SETTINGS.forEach {
                result.addElement(LookupElementBuilder.create(it).withIcon(PlatformIcons.PARAMETER_ICON))
            }
        }

    }
}