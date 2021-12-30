package com.jetbrains.snakecharm.stringLanguage.lang

import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLSubscriptionExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLSubscriptionIndexKeyExpression

interface SmkSLElementVisitor {
    /**
     * Adapter instead of inheritance for to python specific methods
     */
    val pyElementVisitor: PyElementVisitor

    fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpression) {
        pyElementVisitor.visitPyElement(expr)
    }

    fun visitSmkSLSubscriptionExpressionKey(expr: SmkSLSubscriptionIndexKeyExpression) {
        pyElementVisitor.visitPyElement(expr)
    }

    fun visitSmkSLSubscriptionExpression(expr: SmkSLSubscriptionExpression) {
        pyElementVisitor.visitPyElement(expr)
    }
}