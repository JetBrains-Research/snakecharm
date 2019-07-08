package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.codeInsight.controlflow.ScopeOwner
import com.jetbrains.python.documentation.docstrings.DocStringUtil
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyElementImpl



class SMKRuleRunParameter(node: ASTNode): PyElementImpl(node),
        SMKRuleSection,
        ScopeOwner, // for control flow
        PyStatement, PyStatementListContainer, PyDocStringOwner
{
    companion object {
        const val PARAM_NAME = "run"
    }

    override fun getStatementList(): PyStatementList =
            childToPsi(PyElementTypes.STATEMENT_LIST) ?: error("Statement list missing for run section $text")

    fun getNameNode() = getIdentifierNode(node)

    val section: PsiElement?
        get() = firstChild

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKRuleRunParameter(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }

    /**
     * TODO: Think should we use [CachedStructuredDocStringProvider] like in PyFunctionImpl
     **/
    override fun getStructuredDocString() = DocStringUtil.getStructuredDocString(this)
    override fun getDocStringExpression() = DocStringUtil.findDocStringExpression(statementList)
    override fun getDocStringValue() = DocStringUtil.getDocStringValue(this)
}