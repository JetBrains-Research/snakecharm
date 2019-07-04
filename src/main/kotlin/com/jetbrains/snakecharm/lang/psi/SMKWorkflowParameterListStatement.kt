package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */
class SMKWorkflowParameterListStatement(node: ASTNode) : PyElementImpl(node), PyStatement { // PyNamedElementContainer
    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKWorkflowParameterListStatement(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }

    fun getKeywordNode() = node.findChildByType(SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_PARAMLISTS_DECORATOR_KEYWORDS)
}