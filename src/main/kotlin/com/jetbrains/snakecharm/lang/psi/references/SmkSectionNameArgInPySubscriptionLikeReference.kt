package com.jetbrains.snakecharm.lang.psi.references

import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType

class SmkSectionNameArgInPySubscriptionLikeReference(
        element: PyStringLiteralExpression,
        val type: SmkAvailableForSubscriptionType
): PsiPolyVariantReferenceBase<PyStringLiteralExpression>(element) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val resolveResults = arrayListOf<RatedResolveResult>()

        val canonicalText = element.stringValue
        type.resolveMember(
                canonicalText,
                element,
                AccessDirection.READ,
                PyResolveContext.defaultContext()
        )?.let {
            resolveResults.addAll(it)
        }

        return resolveResults.toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        val variants = type.getCompletionVariants(canonicalText, element, ProcessingContext())
        return variants ?: emptyArray()
    }
}