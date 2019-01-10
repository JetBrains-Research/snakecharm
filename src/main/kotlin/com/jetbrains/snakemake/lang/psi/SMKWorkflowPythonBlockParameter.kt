package com.jetbrains.snakemake.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.psi.PyStatementList
import com.jetbrains.python.psi.PyStatementListContainer
import com.jetbrains.python.psi.impl.PyElementImpl

class SMKWorkflowPythonBlockParameter(node: ASTNode): PyElementImpl(node), PyStatementListContainer { // PyNamedElementContainer

    override fun getStatementList(): PyStatementList =
            childToPsi(PyElementTypes.STATEMENT_LIST) ?: error("Statement list missing for workflow parameter $text")
}