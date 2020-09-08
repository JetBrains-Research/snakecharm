package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.SmkReferenceExpression
import com.jetbrains.snakecharm.lang.psi.impl.refs.SmkRuleOrCheckpointNameReference


class SmkReferenceExpressionImpl(node: ASTNode): PyReferenceExpressionImpl(node), SmkReferenceExpression {
    override fun getName() = nameElement?.text

    override fun getNameElement(): ASTNode? = node.findChildByType(PyTokenTypes.IDENTIFIER)
    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key): PyType? = null

    override fun getReference(context: PyResolveContext) =
            SmkRuleOrCheckpointNameReference(this, context)

    override fun toString() = "SmkReferenceExpression: ${this.referencedName}"
}

