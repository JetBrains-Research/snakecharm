package com.jetbrains.snakecharm.string_language.lang.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.PyType

class SmkSLSubscriptionKeyReference(
        private val element: SmkSLSubscriptionExpression,
        private val type: PyType,
        textRange: TextRange
) : PsiReferenceBase<SmkSLSubscriptionExpression>(element, textRange) {
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
}
