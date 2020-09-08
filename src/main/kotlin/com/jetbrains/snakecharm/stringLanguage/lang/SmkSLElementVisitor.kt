package com.jetbrains.snakecharm.stringLanguage.lang

import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLLanguageElement
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLReferenceExpressionImpl
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLSubscriptionExpression
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLSubscriptionKeyReferenceExpression

interface SmkSLElementVisitor {
    /**
     * Adapter instead of inheritance for to python specific methods
     */
    val pyElementVisitor: PyElementVisitor

    fun visitSmkSLLanguageElement(element: SmkSLLanguageElement) {
        pyElementVisitor.visitPyElement(element)
    }

    fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpressionImpl) {
        pyElementVisitor.visitPyElement(expr)
    }

    fun visitSmkSLSubscriptionKeyExpression(expr: SmkSLSubscriptionKeyReferenceExpression) {
        pyElementVisitor.visitPyElement(expr)
    }

    fun visitSmkSLSubscriptionExpression(expr: SmkSLSubscriptionExpression) {
        pyElementVisitor.visitPyElement(expr)
    }
}