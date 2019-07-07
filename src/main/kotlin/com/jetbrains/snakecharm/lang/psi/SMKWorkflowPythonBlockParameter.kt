package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.codeInsight.controlflow.ScopeOwner
import com.jetbrains.python.documentation.docstrings.DocStringUtil
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_PYTHON_BLOCK_PARAMETER_KEYWORDS

class SMKWorkflowPythonBlockParameter(node: ASTNode) : PyElementImpl(node),
        ScopeOwner, // for control flow
        PyStatement, PyStatementListContainer, PyDocStringOwner
{

    override fun getStatementList(): PyStatementList =
            childToPsi(PyElementTypes.STATEMENT_LIST) ?: error("Statement list missing for workflow parameter $text")

    fun getKeywordNode() = node.findChildByType(WORKFLOW_TOPLEVEL_PYTHON_BLOCK_PARAMETER_KEYWORDS)

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKWorkflowPythonBlockParameter(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }

    /**
     * TODO: Think should we use [CachedStructuredDocStringProvider] like in PyFunctionImpl
     **/
    override fun getStructuredDocString() = DocStringUtil.getStructuredDocString(this)
    override fun getDocStringExpression() = DocStringUtil.findDocStringExpression(statementList)
    override fun getDocStringValue() = DocStringUtil.getDocStringValue(this)
}