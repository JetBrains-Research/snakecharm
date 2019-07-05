package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.codeInsight.controlflow.ScopeOwner
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStatementList
import com.jetbrains.python.psi.PyStatementListContainer
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_PYTHON_BLOCK_PARAMETER_KEYWORDS

class SMKWorkflowPythonBlockParameter(node: ASTNode) : PyElementImpl(node), PyStatementListContainer,
        ScopeOwner // for control flow
        // PsiNamedElement // only for PyControlFlowBuilder.visitPyElement() //TODO: marker interface required!
{ // PyNamedElementContainer

    override fun getStatementList(): PyStatementList =
            childToPsi(PyElementTypes.STATEMENT_LIST) ?: error("Statement list missing for workflow parameter $text")

    fun getKeywordNode() = node.findChildByType(WORKFLOW_TOPLEVEL_PYTHON_BLOCK_PARAMETER_KEYWORDS)

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKWorkflowPythonBlockParameter(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
}