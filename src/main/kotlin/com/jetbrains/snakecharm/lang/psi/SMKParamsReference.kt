package com.jetbrains.snakecharm.lang.psi

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.parentOfType
import com.intellij.util.PlatformIcons


class SMKParamsReference(
        element: PsiElement, textRange: TextRange
) : PsiReferenceBase<PsiElement>(element, textRange) {
    private val key: String = element.text.substring(textRange.startOffset, textRange.endOffset)

    override fun resolve() =
            getKeywordArguments().firstOrNull { it.name == key }

    override fun getVariants() =
            getKeywordArguments()
                    .mapNotNull { arg -> arg.keyword }
                    .map { keyword ->
                        LookupElementBuilder.create(keyword).withIcon(PlatformIcons.PARAMETER_ICON)
                    }
                    .toTypedArray()

    private fun getParamsSection() =
            element.parentOfType<SmkRuleLike<SMKRuleParameterListStatement>>()
                    ?.getSectionByName(SMKRuleParameterListStatement.PARAMS)

    private fun getKeywordArguments() =
            getParamsSection()?.keywordArguments ?: emptyList()
}