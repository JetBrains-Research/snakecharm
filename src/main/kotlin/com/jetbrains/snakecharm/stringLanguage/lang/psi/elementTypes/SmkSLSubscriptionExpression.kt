package com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext

class SmkSLSubscriptionExpression(node: ASTNode) : SmkSLElementImpl(node), PyExpression {
    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key): PyType? = null
}
