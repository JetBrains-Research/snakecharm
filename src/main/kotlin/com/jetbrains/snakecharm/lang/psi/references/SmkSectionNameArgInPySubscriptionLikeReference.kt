package com.jetbrains.snakecharm.lang.psi.references

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PsiReferenceEx
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLSubscriptionKeyReference

class SmkSectionNameArgInPySubscriptionLikeReference(
    element: PyStringLiteralExpression,
    val type: SmkAvailableForSubscriptionType,
) : PsiPolyVariantReferenceBase<PyStringLiteralExpression>(element), PsiReferenceEx {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val resolveResults = arrayListOf<RatedResolveResult>()

        val context = TypeEvalContext.codeAnalysis(element.project, element.containingFile)
        val resolveContext = PyResolveContext.defaultContext(context)

        val canonicalText = element.stringValue
        type.resolveMember(
            canonicalText,
            element,
            AccessDirection.READ,
            resolveContext
        )?.let {
            resolveResults.addAll(it)
        }

        return resolveResults.toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        val variants = type.getCompletionVariants(canonicalText, element, ProcessingContext())
        return variants ?: emptyArray()
    }

    override fun getUnresolvedDescription(): String =
        SmkSLSubscriptionKeyReference.unresolvedErrorMsg(element)

    override fun getUnresolvedHighlightSeverity(context: TypeEvalContext?): HighlightSeverity =
        SmkSLSubscriptionKeyReference.INSPECTION_SEVERITY
}