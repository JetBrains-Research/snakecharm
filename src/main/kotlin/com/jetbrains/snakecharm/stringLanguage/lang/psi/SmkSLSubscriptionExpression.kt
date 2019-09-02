package com.jetbrains.snakecharm.stringLanguage.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.stringLanguage.lang.SmkSLElementVisitor

class SmkSLSubscriptionExpression(node: ASTNode) : SmkSLElementImpl(node), PyExpression {
    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key): PyType? = null

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkSLElementVisitor -> pyVisitor.visitSmkSLSubscriptionExpression(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
}
