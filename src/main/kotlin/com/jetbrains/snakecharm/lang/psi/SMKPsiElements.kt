package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor

class SMKRule(node: ASTNode): SmkRuleLike(node) {
    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKRule(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
}

class SMKCheckPoint(node: ASTNode) : SmkRuleLike(node) {
    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKCheckPoint(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
}

class SmkSubworkflow(node: ASTNode): SmkRuleLike(node) {
    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) {
        when (pyVisitor) {
            is SMKElementVisitor -> pyVisitor.visitSMKSubworkflow(this)
            else -> super.acceptPyVisitor(pyVisitor)
        }
    }
}