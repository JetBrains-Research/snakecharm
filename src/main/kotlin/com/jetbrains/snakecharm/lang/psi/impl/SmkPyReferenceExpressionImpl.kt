package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import com.jetbrains.python.psi.impl.references.PyQualifiedReference
import com.jetbrains.python.psi.impl.references.PyReferenceImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.snakecharm.lang.psi.SmkRunSection
import com.jetbrains.snakecharm.lang.psi.SmkSection
import com.jetbrains.snakecharm.lang.psi.impl.refs.SmkPyQualifiedReference
import com.jetbrains.snakecharm.lang.psi.impl.refs.SmkPyReferenceImpl

class SmkPyReferenceExpressionImpl(astNode: ASTNode): PyReferenceExpressionImpl(astNode) {
    override fun getReference(context: PyResolveContext): PsiPolyVariantReference {
        val baseRef = super.getReference(context)

        val qualified = isQualified
        if (qualified && baseRef.javaClass == PyQualifiedReference::class.java) {
            return SmkPyQualifiedReference(this, context)
        }
        
        // delegate to super impl for all custom references
        if (qualified || baseRef.javaClass != PyReferenceImpl::class.java) {
            return baseRef
        }

        // override default impl
        val runSection = PsiTreeUtil.getParentOfType(
                this, SmkRunSection::class.java, true, SmkSection::class.java
        )
        return SmkPyReferenceImpl(this, context, runSection != null)
    }
}