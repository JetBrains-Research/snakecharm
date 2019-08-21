package com.jetbrains.snakecharm.string_language.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType

class SmkSLSubscriptionExpression(node: ASTNode) : SmkSLElement(node), PyExpression {
    private val keyExpression = PsiTreeUtil.getChildOfType(this, SmkSLElement::class.java)

    override fun getReference(): PsiReference? {
        if (keyExpression == null) {
            return null
        }

        val qualifier = children.firstOrNull { it is SmkSLReferenceExpression }
                as? PyTypedElement ?: return null
        val type = PyResolveContext.defaultContext().typeEvalContext.getType(qualifier)

        if (type !is SmkAvailableForSubscriptionType) {
            return null
        }

        return SmkSLSubscriptionKeyReference(this, type, keyExpression.textRangeInParent)
    }

    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key): PyType? = null
}
