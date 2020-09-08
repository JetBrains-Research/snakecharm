package com.jetbrains.snakecharm.stringLanguage.lang.psi.references

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.PsiReferenceEx
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.types.SmkWildcardsType
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpressionImpl

class SmkSLWildcardReference(
        element: SmkSLReferenceExpressionImpl,
        private val type: SmkWildcardsType
) : PsiPolyVariantReferenceBase<SmkSLReferenceExpressionImpl>(element), SmkSLBaseReference, PsiReferenceEx {

    override fun handleElementRename(newElementName: String) =
            element.setName(newElementName)

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> =
            type.resolveMember(
                    wildcardName(),
                    element,
                    AccessDirection.READ,
                    PyResolveContext.defaultContext()
            ).toTypedArray()

    /**
     * not canonical text!!!! (because range will be wrong for "foo.boo.doo" wildcard name
     */
    fun wildcardName() = getWildcardTrueExpression().text

    /**
     * In case of wildcards with dots inside, which is allows but very confusing, we need to take the
     * leftmost qualifier, e.g. "foo.boo.doo.coo", for 'doo' it will be 'foo'. And use it's text as wildcard
     * name
     */
    fun getWildcardTrueExpression() =
            PsiTreeUtil.getTopmostParentOfType(element, SmkSLExpression::class.java) ?: element

    override fun getVariants(): Array<out Any> =
            type.getCompletionVariants(canonicalText, element, ProcessingContext())

    override fun getUnresolvedHighlightSeverity(context: TypeEvalContext?): HighlightSeverity? = null

    override fun getUnresolvedDescription(): String? = null
}
