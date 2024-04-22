package com.jetbrains.snakecharm.stringLanguage.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.lang.psi.types.SmkAvailableForSubscriptionType
import com.jetbrains.snakecharm.stringLanguage.lang.SmkSLElementVisitor
import com.jetbrains.snakecharm.stringLanguage.lang.parser.SmkSLTokenTypes
import com.jetbrains.snakecharm.stringLanguage.lang.psi.references.SmkSLSubscriptionKeyReference

class SmkSLSubscriptionIndexKeyExpressionImpl(node: ASTNode) : PyReferenceExpressionImpl(node),
    SmkSLSubscriptionIndexKeyExpression {
    override fun getName() = referencedName

    override fun getNameElement() = node.findChildByType(SmkSLTokenTypes.ACCESS_KEY)

    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key): PyType? = null

    override fun getReference(): PsiPolyVariantReference {
        val type = qualifier?.let {
            // Use same eval context as in PyReferenceExpressionImpl.getReference(), maybe it is more correct
            val context = TypeEvalContext.codeAnalysis(this.project, this.containingFile)
            context.getType(it)
        }
        val subscriptionType = when (type) {
            is SmkAvailableForSubscriptionType -> type
            else -> null
        }
        return SmkSLSubscriptionKeyReference(this, subscriptionType)
    }

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkSLElementVisitor -> pyVisitor.visitSmkSLSubscriptionExpressionKey(this)
        else -> super<PyReferenceExpressionImpl>.acceptPyVisitor(pyVisitor)
    }


    override fun getReference(context: PyResolveContext): PsiPolyVariantReference = reference

    override fun getQualifier() =
        PsiTreeUtil.getParentOfType(this, SmkSLSubscriptionExpressionImpl::class.java)?.getOperand()

    override fun toString() = "SmkSLSubscriptionKeyExpression: [${this.referencedName}]"
}