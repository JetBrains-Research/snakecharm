package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor

class SMKSubworkflowParameterListStatement(node: ASTNode): SmkSectionStatement(node) {
    companion object {
        val PARAMS_NAMES = setOf(
                "workdir", "snakefile", "configfile"
        )
    }

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) {
        when (pyVisitor) {
            is SMKElementVisitor -> pyVisitor.visitSMKSubworkflowParameterListStatement(this)
            else -> super.acceptPyVisitor(pyVisitor)
        }
    }
}