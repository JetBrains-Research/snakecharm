package com.jetbrains.snakecharm.lang.psi

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.parentOfType
import com.jetbrains.python.psi.resolve.RatedResolveResult
import java.util.*


class SMKParamsReference(
        element: PsiElement, textRange: TextRange
) : PsiReferenceBase<PsiElement>(element, textRange), PsiPolyVariantReference {
    private val key: String = element.text.substring(textRange.startOffset, textRange.endOffset)

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val paramsSection = getParamsSection() ?: return emptyArray()
        paramsSection.argumentList?.arguments?.forEach {
            if (it.name == this.key) {
                return arrayOf(RatedResolveResult(RatedResolveResult.RATE_NORMAL, it))
            }
        }
        return emptyArray()
    }

    override fun resolve(): PsiElement? {
        val resolveResults = multiResolve(false)
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun getVariants(): Array<Any> {
        val paramsSection = getParamsSection() ?: return emptyArray()
        val variants = ArrayList<LookupElement>()
        paramsSection.argumentList?.arguments?.forEach {
            variants.add(LookupElementBuilder.create(it.name!!))
        }
        return variants.toTypedArray()
    }

    private fun getParamsSection(): SMKRuleParameterListStatement? {
        val rule = this.element.parentOfType<SMKRule>() ?: return null
        return rule.statementList.statements
                ?.find {
                    (it as SMKRuleParameterListStatement)
                            .section
                            .textMatches(SMKRuleParameterListStatement.PARAMS)
                }
                as? SMKRuleParameterListStatement
    }
}