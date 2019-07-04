package com.jetbrains.snakecharm.lang.psi

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.parentOfType
import com.intellij.util.PlatformIcons
import com.intellij.util.containers.toMutableSmartList


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
        return (getKeywordArguments()
                ?.map { arg ->
                    LookupElementBuilder.create(arg.keyword!!).withIcon(PlatformIcons.PARAMETER_ICON)
                } ?: emptyList()).toMutableSmartList().toTypedArray()
    }

    private fun getParamsSection(): SMKRuleParameterListStatement? {
        val rule = this.element.parentOfType<SMKRule>() ?: return null
        return rule.getSectionByName(SMKRuleParameterListStatement.PARAMS)
    }

    private fun getKeywordArguments() = getParamsSection()?.keywordArguments
}