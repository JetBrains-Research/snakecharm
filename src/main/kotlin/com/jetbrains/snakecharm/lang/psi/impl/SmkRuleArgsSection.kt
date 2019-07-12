package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.snakecharm.lang.psi.SMKElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkRuleArgsSection

class SmkRuleArgsSectionImpl(node: ASTNode): SmkArgsSectionImpl(node), SmkRuleArgsSection {
    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKRuleParameterListStatement(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
}