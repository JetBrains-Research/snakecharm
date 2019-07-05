package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.codeInsight.controlflow.ScopeOwner
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStatementList
import com.jetbrains.python.psi.PyStatementListContainer
import com.jetbrains.python.psi.impl.PyElementImpl

class SMKRuleRunParameter(node: ASTNode): PyElementImpl(node), PyStatementListContainer,
        SMKRuleSection,
        ScopeOwner // for control flow
{ // PyNamedElementContainer
    companion object {
        const val PARAM_NAME = "run"
    }

    override fun getStatementList(): PyStatementList =
            childToPsi(PyElementTypes.STATEMENT_LIST) ?: error("Statement list missing for workflow parameter $text")

    fun getNameNode() = getIdentifierNode(node)

    val section: PsiElement?
        get() = firstChild

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKRuleRunParameter(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }
}