package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.ast.docstring.DocStringUtilCore
import com.jetbrains.python.documentation.docstrings.DocStringUtil
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStatementList
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.WORKFLOW_TOPLEVEL_PYTHON_BLOCK_PARAMETER_KEYWORDS
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkWorkflowPythonBlockSection
import com.jetbrains.snakecharm.lang.psi.getIcon
import com.jetbrains.snakecharm.lang.psi.getPresentation

class SmkWorkflowPythonBlockSectionImpl(node: ASTNode) : PyElementImpl(node), SmkWorkflowPythonBlockSection {
    @Suppress("UnstableApiUsage")
    override fun getStatementList(): PyStatementList =
            childToPsi(PyElementTypes.STATEMENT_LIST) ?: error("Statement list missing for workflow parameter $text")

    override fun getSectionKeywordNode() = node.findChildByType(WORKFLOW_TOPLEVEL_PYTHON_BLOCK_PARAMETER_KEYWORDS)

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkElementVisitor -> pyVisitor.visitSmkWorkflowPythonBlockSection(this)
        else -> super<PyElementImpl>.acceptPyVisitor(pyVisitor)
    }

    override fun getPresentation() = getPresentation(this)
    override fun getIcon(flags: Int) = getIcon(this, flags)

    /**
     * TODO: Think should we use CachedStructuredDocStringProvider like in [com.jetbrains.python.psi.impl.PyFunctionImpl]
     **/
    override fun getStructuredDocString() = DocStringUtil.getStructuredDocString(this)
    override fun getDocStringExpression() = DocStringUtil.findDocStringExpression(statementList)
    @Suppress("UnstableApiUsage")
    override fun getDocStringValue() = DocStringUtilCore.getDocStringValue(this)
}