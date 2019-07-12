package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes

class SmkWorkflowRuleorderSectionImpl(node: ASTNode): PyElementImpl(node), SmkWorkflowRuleorderSection {
    override fun getSectionKeywordNode() = node.findChildByType(SnakemakeTokenTypes.WORKFLOW_RULEORDER_KEYWORD)

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKWorkflowRuleOrderStatement(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
}