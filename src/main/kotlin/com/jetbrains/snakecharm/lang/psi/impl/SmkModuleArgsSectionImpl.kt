package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkModuleArgsSection

class SmkModuleArgsSectionImpl(node: ASTNode) : SmkArgsSectionImpl(node), SmkModuleArgsSection {
    override fun acceptPyVisitor(pyVisitor: PyElementVisitor?) {
        when (pyVisitor) {
            is SmkElementVisitor -> pyVisitor.visitSmkModuleArgsSection(this)
            else -> super.acceptPyVisitor(pyVisitor)
        }
    }
}