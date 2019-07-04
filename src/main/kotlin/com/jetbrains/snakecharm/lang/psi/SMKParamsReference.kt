package com.jetbrains.snakecharm.lang.psi

import com.intellij.codeInsight.lookup.LookupElement
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

    override fun resolve(): PsiElement? {
        getKeywordArguments()?.forEach {
            if (it.name == this.key) {
                return it
            }
        }
        return null
    }

    override fun getVariants(): Array<Any> {
        val variants = mutableListOf<LookupElement>()
        getKeywordArguments()?.forEach {
            variants.add(LookupElementBuilder.create(it.name!!).withIcon(PlatformIcons.PARAMETER_ICON))
        }
        return variants.toTypedArray()
    }

    private fun getParamsSection(): SMKRuleParameterListStatement? {
        val rule = this.element.parentOfType<SMKRule>() ?: return null
        return rule.getSectionByName(SMKRuleParameterListStatement.PARAMS)
    }

    private fun getKeywordArguments() = getParamsSection()?.keywordArguments
}