package com.jetbrains.snakecharm.stringLanguage

import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkSLReferenceExpression

interface SmkSLElementVisitor {
    /**
     * Adapter instead of inheritance for to python specific methods
     */
    val pyElementVisitor: PyElementVisitor

    fun visitSmkSLReferenceExpression(expr: SmkSLReferenceExpression) {
        pyElementVisitor.visitPyElement(expr)
    }
}