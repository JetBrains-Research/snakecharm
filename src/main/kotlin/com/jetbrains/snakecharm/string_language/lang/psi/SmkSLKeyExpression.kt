package com.jetbrains.snakecharm.string_language.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import com.jetbrains.python.psi.impl.references.PyQualifiedReference
import com.jetbrains.python.psi.impl.references.PyReferenceImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType

class SmkSLKeyExpression(node: ASTNode) : PyReferenceExpressionImpl(node) {
    // The name is not qualified since it can contain dots
    override fun asQualifiedName(): QualifiedName? = null

    override fun getReference(context: PyResolveContext): PsiPolyVariantReference {
        val type = context.typeEvalContext.getType(qualifier as PyTypedElement)
        if (type is SmkAvailableForSubscriptionType) {
            return PyQualifiedReference(this, context)
        }

        return PyReferenceImpl(this, context)
    }

    override fun getQualifier() =
            PsiTreeUtil.getParentOfType(this, SmkSLSubscriptionExpression::class.java)
                    ?.children?.firstOrNull { it is SmkSLReferenceExpression } as PyExpression?
}
