package com.jetbrains.snakecharm.stringLanguage.lang.psi.references

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PsiReferenceEx
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLKeyExpression

class SmkSLSubscriptionKeyReference(
        private val element: SmkSLKeyExpression,
        private val type: PyType
) : PsiReferenceBase<SmkSLKeyExpression>(element), PsiReferenceEx {
    override fun getUnresolvedDescription(): String =
            SnakemakeBundle.message("INSP.NAME.unresolved.subscription.ref", element.text)

    override fun getUnresolvedHighlightSeverity(context: TypeEvalContext?): HighlightSeverity =
            HighlightSeverity.WEAK_WARNING

    override fun handleElementRename(newElementName: String) =
            element.setName(newElementName)

    override fun resolve(): PsiElement? {
        val resolveResults = type.resolveMember(
                canonicalText,
                element,
                AccessDirection.READ,
                PyResolveContext.defaultContext()
        ) ?: return null

        if (resolveResults.isEmpty()) {
            return null
        }

        return resolveResults.first()?.element
    }

    override fun getVariants(): Array<Any> =
            type.getCompletionVariants(canonicalText, element, ProcessingContext())

    override fun calculateDefaultRangeInElement() = TextRange.create(0, element.textLength)
}
