package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes
import com.jetbrains.snakecharm.lang.validation.SnakemakeAnnotator

class SMKWorkflowLocalRulesStatement(node: ASTNode): PyElementImpl(node), PyStatement {
    fun getKeywordNode() = node.findChildByType(SnakemakeTokenTypes.WORKFLOW_LOCALRULES_KEYWORD)

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) {
        if (pyVisitor is SnakemakeAnnotator) {
            pyVisitor.visitSMKWorkflowLocalRulesStatement(this)
        } else {
            super.acceptPyVisitor(pyVisitor)
        }
    }
}