package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkSubworkflowArgsSection

class SmkSubworkflowArgsSectionImpl(node: ASTNode): SmkArgsSectionImpl(node), SmkSubworkflowArgsSection {
    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) {
        when (pyVisitor) {
            is SmkElementVisitor -> pyVisitor.visitSmkSubworkflowArgsSection(this)
            else -> super<SmkArgsSectionImpl>.acceptPyVisitor(pyVisitor)
        }
    }
}