package com.jetbrains.snakecharm.stringLanguage.lang

import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpressionImpl
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLSubscriptionExpressionImpl
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLSubscriptionIndexKeyExpressionImpl

interface SmkSLElementVisitor {
    /**
     * Adapter instead of inheritance for to python specific methods
     */
    val pyElementVisitor: PyElementVisitor

    fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpressionImpl) {
        pyElementVisitor.visitPyElement(expr)
    }

    fun visitSmkSLSubscriptionExpressionKey(expr: SmkSLSubscriptionIndexKeyExpressionImpl) {
        pyElementVisitor.visitPyElement(expr)
    }

    fun visitSmkSLSubscriptionExpression(expr: SmkSLSubscriptionExpressionImpl) {
        pyElementVisitor.visitPyElement(expr)
    }
}