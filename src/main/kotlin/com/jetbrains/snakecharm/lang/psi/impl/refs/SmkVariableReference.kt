package com.jetbrains.snakecharm.lang.psi.impl.refs

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReferenceBase
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.PyTargetExpression

class SmkVariableReference(
        element: PyStringLiteralExpression,
        textRange: TextRange,
        private val variable: PyTargetExpression?
) : PsiReferenceBase<PyStringLiteralExpression>(element, textRange) {

    override fun resolve() = variable
}