package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes

class SmkWorkflowLocalrulesSectionImpl(node: ASTNode): PyElementImpl(node), SmkWorkflowLocalrulesSection {
    override fun getSectionKeywordNode() = node.findChildByType(SnakemakeTokenTypes.WORKFLOW_LOCALRULES_KEYWORD)

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKWorkflowLocalRulesStatement(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
}