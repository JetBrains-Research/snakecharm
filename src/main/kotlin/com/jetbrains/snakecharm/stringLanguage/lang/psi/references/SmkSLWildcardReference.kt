package com.jetbrains.snakecharm.stringLanguage.lang.psi.references

import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.util.ProcessingContext
import com.jetbrains.python.psi.AccessDirection
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.snakecharm.lang.psi.types.SmkWildcardsType
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLReferenceExpressionImpl

class SmkSLWildcardReference(
        private  val element: SmkSLReferenceExpressionImpl,
        private val type: SmkWildcardsType
) : PsiPolyVariantReferenceBase<SmkSLReferenceExpressionImpl>(element) {
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
}
