package com.jetbrains.snakecharm.stringLanguage.lang.psi.references

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.util.ArrayUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PsiReferenceEx
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.completion.SmkDictionaryTypesCompletionProvider
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLSubscriptionIndexKeyExpressionImpl

class SmkSLSubscriptionKeyReference(
    private val element: SmkSLSubscriptionIndexKeyExpressionImpl,
    val type: SmkAvailableForSubscriptionType?,
) : PsiPolyVariantReferenceBase<SmkSLSubscriptionIndexKeyExpressionImpl>(element), PsiReferenceEx, SmkSLBaseReference {
    companion object {
        fun indexArgTypeText(type: SmkAvailableForSubscriptionType) =
            SnakemakeBundle.message("TYPES.rule.section.arg.index.type.text", type.name!!)

        fun unresolvedErrorMsg(element: PsiElement) =
            SnakemakeBundle.message("INSP.NAME.unresolved.subscription.ref", element.text)

        val INSPECTION_SEVERITY = HighlightSeverity.WARNING!!
    }

    override fun getUnresolvedDescription(): String = unresolvedErrorMsg(element)

    override fun getUnresolvedHighlightSeverity(context: TypeEvalContext?): HighlightSeverity? =
        when (type) {
            null -> null
            else -> INSPECTION_SEVERITY
        }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        type ?: return ResolveResult.EMPTY_ARRAY

        val context = TypeEvalContext.codeAnalysis(element.project, element.containingFile)
        val resolveContext = PyResolveContext.defaultContext(context)
        val accessDirection = AccessDirection.READ

        val resolveResults = arrayListOf<RatedResolveResult>()
        type.resolveMember(
            canonicalText, element, accessDirection, resolveContext
        )?.let {
            resolveResults.addAll(it)
        }

        if (type.getPositionArgsNumber(element) > 0) {
            canonicalText.toIntOrNull()?.let { idx ->
                resolveResults.addAll(type.resolveMemberByIndex(
                    idx, element, accessDirection, resolveContext
                ))
            }
        }

        if (resolveResults.isEmpty()) {
            return ResolveResult.EMPTY_ARRAY
        }
        return resolveResults.toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        type ?: return ArrayUtil.EMPTY_OBJECT_ARRAY

        val variants = arrayListOf<Any>()
        variants.addAll(type.getCompletionVariants(canonicalText, element, ProcessingContext()))
        variants.addAll(SmkDictionaryTypesCompletionProvider.collectLookupItemsForPositionArgs(type, element))
        return variants.toTypedArray()
    }

    override fun calculateDefaultRangeInElement() = TextRange.create(0, element.textLength)

    override fun handleElementRename(newElementName: String) =
        element.setName(newElementName)
}
