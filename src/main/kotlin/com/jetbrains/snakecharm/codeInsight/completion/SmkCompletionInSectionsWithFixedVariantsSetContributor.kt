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

class SmkCompletionInSectionsWithFixedVariantsSetContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            ShadowSectionSettingsProvider.CAPTURE,
            ShadowSectionSettingsProvider
        )
        extend(
            CompletionType.BASIC,
            TemplateEngineSettingsProvider.CAPTURE,
            TemplateEngineSettingsProvider
        )
    }
}

object TemplateEngineSettingsProvider : CompletionProvider<CompletionParameters>() {
    val CAPTURE = PlatformPatterns.psiElement()
        .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
        .inside(SmkRuleOrCheckpointArgsSection::class.java)
        .inside(PyStringLiteralExpression::class.java)!!

    val OPTIONS = listOf("yte", "jinja2")
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val parentListStatement =
            PsiTreeUtil.getParentOfType(parameters.position, SmkRuleOrCheckpointArgsSection::class.java)!!
        if (parentListStatement.name == SnakemakeNames.SECTION_TEMPLATE_ENGINE) {
            OPTIONS.forEach {
                result.addElement(LookupElementBuilder.create(it).withIcon(PlatformIcons.PARAMETER_ICON))
            }
        }
    }
}

object ShadowSectionSettingsProvider : CompletionProvider<CompletionParameters>() {
    val CAPTURE = PlatformPatterns.psiElement()
        .inFile(SmkKeywordCompletionContributor.IN_SNAKEMAKE)
        .inside(SmkRuleOrCheckpointArgsSection::class.java)
        .inside(PyStringLiteralExpression::class.java)!!

    val OPTIONS = listOf("shallow", "full", "minimal", "copy-minimal")

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val parentListStatement =
            PsiTreeUtil.getParentOfType(parameters.position, SmkRuleOrCheckpointArgsSection::class.java)!!
        if (parentListStatement.name == SnakemakeNames.SECTION_SHADOW) {
            OPTIONS.forEach {
                result.addElement(LookupElementBuilder.create(it).withIcon(PlatformIcons.PARAMETER_ICON))
            }
        }

    }
}