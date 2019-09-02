package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import com.jetbrains.python.psi.impl.references.PyReferenceImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.snakecharm.lang.psi.SmkRunSection
import com.jetbrains.snakecharm.lang.psi.SmkSection
import com.jetbrains.snakecharm.lang.psi.impl.refs.SmkPyReferenceImpl

class SmkPyReferenceExpressionImpl(astNode: ASTNode): PyReferenceExpressionImpl(astNode) {
    override fun getReference(context: PyResolveContext): PsiPolyVariantReference {
        val baseRef = super.getReference(context)

        // delegate to super impl for all custom references
        if (isQualified || baseRef.javaClass != PyReferenceImpl::class.java) {
            return baseRef
        }

        // override default impl
        val runSection = PsiTreeUtil.getParentOfType(
                this, SmkRunSection::class.java, true, SmkSection::class.java
        )
        return SmkPyReferenceImpl(this, context, runSection != null)
    }
}