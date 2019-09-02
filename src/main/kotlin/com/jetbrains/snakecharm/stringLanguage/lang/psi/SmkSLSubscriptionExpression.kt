package com.jetbrains.snakecharm.stringLanguage.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.BaseSmkSLReferenceExpression
import com.jetbrains.snakecharm.stringLanguage.lang.SmkSLElementVisitor

/**
 * Not reference expression
 */
class SmkSLSubscriptionExpression(node: ASTNode) : SmkSLElementImpl(node), SmkSLExpression {
    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key): PyType? = null

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkSLElementVisitor -> pyVisitor.visitSmkSLSubscriptionExpression(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
    
    fun getOperand() = children.firstOrNull { it is BaseSmkSLReferenceExpression } as? BaseSmkSLReferenceExpression
}
