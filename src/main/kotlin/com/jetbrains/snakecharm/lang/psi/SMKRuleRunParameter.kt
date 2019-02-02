package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStatementList
import com.jetbrains.python.psi.PyStatementListContainer
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.validation.SnakemakeAnnotator

class SMKRuleRunParameter(node: ASTNode): PyElementImpl(node), PyStatementListContainer { // PyNamedElementContainer
    companion object {
        const val PARAM_NAME = "run"
    }

    override fun getStatementList(): PyStatementList =
            childToPsi(PyElementTypes.STATEMENT_LIST) ?: error("Statement list missing for workflow parameter $text")

    fun getNameNode() = getIdentifierNode(node)

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) {
            if (pyVisitor is SnakemakeAnnotator) {
                pyVisitor.visitSMKRuleRunParameter(this)
            } else {
                super.acceptPyVisitor(pyVisitor)
            }
        }
}