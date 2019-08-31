package com.jetbrains.snakecharm.stringLanguage.lang.psi.references

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PsiReferenceEx
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.SmkSLReferenceExpression
import com.jetbrains.snakecharm.lang.psi.types.SmkWildcardsType
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpressionImpl

class SmkSLWildcardReference(
        element: SmkSLReferenceExpressionImpl,
        private val type: SmkWildcardsType
) : PsiPolyVariantReferenceBase<SmkSLReferenceExpression>(element), SmkSLBaseReference, PsiReferenceEx {

    override fun handleElementRename(newElementName: String) =
            element.setName(newElementName)

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
            type.resolveMember(
                    canonicalText,
                    element,
                    AccessDirection.READ,
                    PyResolveContext.defaultContext()
            ).toTypedArray()

    override fun getVariants(): Array<out Any> =
            type.getCompletionVariants(canonicalText, element, ProcessingContext())

    override fun getUnresolvedHighlightSeverity(context: TypeEvalContext?): HighlightSeverity? = null

    override fun getUnresolvedDescription(): String? = null
}
