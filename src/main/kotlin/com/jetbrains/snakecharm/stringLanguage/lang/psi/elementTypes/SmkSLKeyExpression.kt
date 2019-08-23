package com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes

import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.SmkSLReferenceExpression
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLSubscriptionKeyReference

class SmkSLKeyExpression(node: ASTNode) : SmkSLElementImpl(node), SmkSLReferenceExpression {
    override fun getName(): String? {
        return super<SmkSLReferenceExpression>.getName()
    }

    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key): PyType? = null

    override fun getReference(): PsiReference? {
        val qualifier = getQualifier() ?: return null
        val type = PyResolveContext.defaultContext().typeEvalContext.getType(qualifier)

        if (type !is SmkAvailableForSubscriptionType) {
            return null
        }

        return SmkSLSubscriptionKeyReference(this, type)
    }

    private fun getQualifier() =
            PsiTreeUtil.getParentOfType(this, SmkSLSubscriptionExpression::class.java)
                    ?.children
                    ?.firstOrNull { it is SmkSLReferenceExpression }
                    as? PyTypedElement
}