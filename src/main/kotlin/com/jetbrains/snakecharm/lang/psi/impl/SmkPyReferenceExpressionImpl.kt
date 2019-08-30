package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiPolyVariantReference
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import com.jetbrains.python.psi.impl.references.PyReferenceImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.snakecharm.lang.psi.impl.refs.SmkPyReferenceImpl

class SmkPyReferenceExpressionImpl(astNode: ASTNode): PyReferenceExpressionImpl(astNode) {
    override fun getReference(context: PyResolveContext): PsiPolyVariantReference {
        val baseRef = super.getReference(context)

        // delegate to super impl for all custom references
        if (isQualified || baseRef.javaClass != PyReferenceImpl::class.java) {
            return baseRef
        }

        // override default impl
        return SmkPyReferenceImpl(this, context)
    }
}