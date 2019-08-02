package com.jetbrains.snakecharm.lang.psi.impl.refs

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.codeInsight.SnakemakeSectionsInShellReferenceContributor
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint


class SmkSectionReference(
        element: PyStringLiteralExpression,
        private val textRange: TextRange,
        private val sectionName: String?
) : PsiReferenceBase<PyStringLiteralExpression>(element, textRange) {
    private val key: String = element.text.substring(textRange.startOffset, textRange.endOffset)

    override fun resolve(): PsiElement? {
        return if (sectionName != null &&
                element.text.substring(textRange.startOffset, textRange.endOffset) != sectionName) {
            getKeywordArguments().firstOrNull { it.name == key }
        } else if (sectionName == element.text.substring(textRange.startOffset, textRange.endOffset)) {
            getSection()
        } else {
            null
        }
    }


    override fun getVariants() =
            if (sectionName != null &&
                    element.text.substring(textRange.startOffset, textRange.endOffset) != sectionName) {
                getKeywordArguments()
                        .mapNotNull { arg -> arg.keyword }
                        .map { keyword ->
                            LookupElementBuilder.create(keyword).withIcon(PlatformIcons.PARAMETER_ICON)
                        }
                        .toTypedArray()
            } else if (sectionName == element.text.substring(textRange.startOffset, textRange.endOffset)) {
                emptyArray()
            } else {
                SnakemakeSectionsInShellReferenceContributor.ALLOWED_IN_SHELL_WITHOUT_KEYWORDS
                        .filter { section ->
                            PsiTreeUtil.getParentOfType(element, SmkRuleOrCheckpoint::class.java)
                                    ?.getSections()
                                    ?.map { it.sectionKeyword }
                                    ?.contains(section)
                                    ?: false
                        }
                        .map { section -> LookupElementBuilder.create(section).withIcon(PlatformIcons.PARAMETER_ICON) }
                        .toTypedArray()
            }

    private fun getSection() =
            if (sectionName == null) {
                null
            } else {
                PsiTreeUtil.getParentOfType(element, SmkRuleOrCheckpoint::class.java)
                        ?.getSectionByName(sectionName)
            }

    private fun getKeywordArguments() =
            getSection()?.keywordArguments ?: emptyList()
}

