package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor

class SmkSubworkflowArgsSectionImpl(node: ASTNode): SmkArgsSectionImpl(node), SmkSubworkflowArgsSection {
    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) {
        when (pyVisitor) {
            is SMKElementVisitor -> pyVisitor.visitSMKSubworkflowParameterListStatement(this)
            else -> super.acceptPyVisitor(pyVisitor)
        }
    }
}