package com.jetbrains.snakecharm.stringLanguage.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.ast.PyAstElementVisitor
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PySubscriptionExpression

@Suppress("UnstableApiUsage")
interface SmkSLSubscriptionExpression : SmkSLReferenceExpression, PySubscriptionExpression {
    override fun acceptPyVisitor(pyVisitor: PyAstElementVisitor?) {
        super<SmkSLReferenceExpression>.acceptPyVisitor(pyVisitor)
    }

    override fun getQualifier(): PyExpression? {
        return super<SmkSLReferenceExpression>.getQualifier()
    }

    override fun isQualified(): Boolean {
        return super<SmkSLReferenceExpression>.isQualified()
    }

    override fun getReferencedName(): String? {
        return super<SmkSLReferenceExpression>.getReferencedName()
    }

    override fun getNameElement(): ASTNode? {
        return super<SmkSLReferenceExpression>.getNameElement()
    }
}