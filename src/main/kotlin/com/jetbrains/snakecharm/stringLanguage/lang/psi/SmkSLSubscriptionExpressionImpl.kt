package com.jetbrains.snakecharm.stringLanguage.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiPolyVariantReference
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.impl.PyEvaluator
import com.jetbrains.python.psi.impl.PyPsiUtils
import com.jetbrains.python.psi.impl.references.PyOperatorReference
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.QualifiedRatedResolveResult
import com.jetbrains.python.psi.resolve.QualifiedResolveResult
import com.jetbrains.python.psi.types.PyTupleType
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.PyTypedDictType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.snakecharm.stringLanguage.lang.SmkSLElementVisitor
import com.jetbrains.snakecharm.stringLanguage.lang.parser.SmkSLTokenTypes
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLElementTypes.EXPRESSION_TOKENS
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLElementTypes.KEY_EXPRESSION
import java.util.function.Predicate

class SmkSLSubscriptionExpressionImpl(node: ASTNode) : SmkSLElementImpl(node), SmkSLSubscriptionExpression {
    //    override fun toString() = "${this::class.java.simpleName}(${node.elementType})"

    override fun getReference(context: PyResolveContext): PsiPolyVariantReference {
        // Default impl from [PySubscriptionExpressionImpl], maybe need to implement own reference here
        // this impl should allow enabling type based features if operand has type defineed
        return PyOperatorReference(this, context)
    }

    override fun getReference(): PsiPolyVariantReference {
        val context = TypeEvalContext.codeAnalysis(project, containingFile)
        val resolveContext = PyResolveContext.defaultContext(context)

        return getReference(resolveContext)
    }

    override fun getType(context: TypeEvalContext, key: TypeEvalContext.Key): PyType? {
        // Taken from [PySubscriptionExpressionImpl]
        val indexExpression = this.indexExpression

        val type = if (indexExpression != null) context.getType(operand) else null

        if (type is PyTupleType) {
            val index = PyEvaluator.evaluate(indexExpression, Int::class.java)
            return when {
                index != null -> type.getElementType(index)
                else -> null
            }
        }
        if (type is PyTypedDictType) {
            val keyStr = PyEvaluator.evaluate(indexExpression, String::class.java)
            return when {
                keyStr != null -> type.getElementType(keyStr)
                else -> null
            }
        }
        // TODO:  PyCallExpressionHelper.getCallType(this, context, key)
        return null
    }

    override fun getQualifier() = operand

    override fun isQualified() = true

    override fun asQualifiedName() = PyPsiUtils.asQualifiedName(this)

    /**
     * Used only to read values
     */
    override fun getReferencedName() = "__getitem__"

    override fun getNameElement() = node.findChildByType(SmkSLTokenTypes.LBRACKET)

    override fun followAssignmentsChain(context: PyResolveContext): QualifiedResolveResult {
        // Assignment chain not expected here, this syntax used only for read access
        return QualifiedResolveResult.EMPTY

    }

    override fun multiFollowAssignmentsChain(
        context: PyResolveContext,
        follow: Predicate<in PyTargetExpression>,
    ): MutableList<QualifiedRatedResolveResult> {
        // Assignment chain not expected here, this syntax used only for read access
        return arrayListOf()
    }

    override fun getOperand() = this.childToPsiNotNull<SmkSLExpression>(EXPRESSION_TOKENS, 0)

    override fun getRootOperand(): SmkSLExpression {
        var current: SmkSLExpression = operand

        while (current is SmkSLSubscriptionExpressionImpl) {
            current = current.operand
        }

        return current
    }

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkSLElementVisitor -> pyVisitor.visitSmkSLSubscriptionExpression(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }

    override fun getIndexExpression() = this.childToPsi<SmkSLSubscriptionIndexKeyExpressionImpl>(KEY_EXPRESSION)
}
