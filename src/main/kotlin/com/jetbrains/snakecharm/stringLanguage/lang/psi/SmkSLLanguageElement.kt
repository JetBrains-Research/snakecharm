package com.jetbrains.snakecharm.stringLanguage.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.stringLanguage.lang.SmkSLElementVisitor


class SmkSLLanguageElement(node: ASTNode) : SmkSLElementImpl(node) {
    fun getExpressions() = children.filterIsInstance<SmkSLExpression>()

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkSLElementVisitor -> pyVisitor.visitSmkSLLanguageElement(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }

}