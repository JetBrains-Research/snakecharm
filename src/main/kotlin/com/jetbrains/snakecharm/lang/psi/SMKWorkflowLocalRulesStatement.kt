package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes

class SMKWorkflowLocalRulesStatement(node: ASTNode): PyElementImpl(node), PyStatement {
    fun getKeywordNode() = node.findChildByType(SnakemakeTokenTypes.WORKFLOW_LOCALRULES_KEYWORD)

    private val argumentList: PyArgumentList?
        get() = children.firstOrNull { it is PyArgumentList } as PyArgumentList?

    val ruleReferences: List<SmkReferenceExpression>?
        get() = argumentList?.children?.mapNotNull { it as SmkReferenceExpression }

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKWorkflowLocalRulesStatement(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
}