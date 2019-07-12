package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.lang.psi.SMKElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflowArgsSection

class SmkSubworkflowArgsSectionImpl(node: ASTNode): SmkArgsSectionImpl(node), SmkSubworkflowArgsSection {
    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) {
        when (pyVisitor) {
            is SMKElementVisitor -> pyVisitor.visitSMKSubworkflowParameterListStatement(this)
            else -> super.acceptPyVisitor(pyVisitor)
        }
    }
}