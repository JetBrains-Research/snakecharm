package com.jetbrains.snakecharm.stringLanguage.lang.psi.references

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.util.ArrayUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PsiReferenceEx
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.completion.SmkCompletionUtil
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLSubscriptionKeyExpression

class SmkSLSubscriptionKeyReference(
        private val element: SmkSLSubscriptionKeyExpression,
        private val type: SmkAvailableForSubscriptionType?
) : PsiPolyVariantReferenceBase<SmkSLSubscriptionKeyExpression>(element), PsiReferenceEx, SmkSLBaseReference {
    override fun getUnresolvedDescription(): String =
            SnakemakeBundle.message("INSP.NAME.unresolved.subscription.ref", element.text)

    override fun getUnresolvedHighlightSeverity(context: TypeEvalContext?): HighlightSeverity? =
            when (type) {
                null -> null
                else -> HighlightSeverity.WARNING
            }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        type ?: return ResolveResult.EMPTY_ARRAY

        val resolveResults = type.resolveMember(
                canonicalText,
                element,
                AccessDirection.READ,
                PyResolveContext.defaultContext()
        ) ?: return ResolveResult.EMPTY_ARRAY

        if (resolveResults.isEmpty()) {
            return ResolveResult.EMPTY_ARRAY
        }
        return resolveResults.toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        type ?: return ArrayUtil.EMPTY_OBJECT_ARRAY

        val variants = arrayListOf<Any>()
        variants.addAll(type.getCompletionVariants(canonicalText, element, ProcessingContext()))

        val typeText = indexArgTypeText(type)

        (0 until type.getPositionArgsNumber(element)).forEach { idx ->
            val item = SmkCompletionUtil.createPrioritizedLookupElement(
                    idx.toString(),
                    PlatformIcons.PARAMETER_ICON,
                    priority = SmkCompletionUtil.SUBSCRIPTION_INDEXES_PRIORITY,
                    typeText = typeText
            )
            variants.add(item)
        }
        return variants.toTypedArray()
    }
    override fun calculateDefaultRangeInElement() = TextRange.create(0, element.textLength)

    override fun handleElementRename(newElementName: String) =
            element.setName(newElementName)

    companion object {
        fun indexArgTypeText(type: SmkAvailableForSubscriptionType) =
                "Arg index in ${type.name}"

    }
}
