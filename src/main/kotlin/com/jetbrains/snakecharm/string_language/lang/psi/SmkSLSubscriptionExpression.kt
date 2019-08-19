package com.jetbrains.snakecharm.string_language.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.impl.PySubscriptionExpressionImpl
import com.jetbrains.snakecharm.string_language.SmkSLTokenTypes

class SmkSLSubscriptionExpression(node: ASTNode) : PySubscriptionExpressionImpl(node) {
    override fun getReference(): PsiReference? = null

    override fun getOperand(): PyExpression =
            childToPsiNotNull(SmkSLTokenTypes.SUBSCRIPTION_KEY_EXPRESSIONS, 0)
}
