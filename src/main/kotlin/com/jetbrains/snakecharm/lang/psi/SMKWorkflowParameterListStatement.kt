package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */
class SMKWorkflowParameterListStatement(node: ASTNode) : PyElementImpl(node), PyStatement { // PyNamedElementContainer
    companion object {
        val WORKFLOWS_WITH_FILE_REFERENCES = setOf(
                SnakemakeNames.WORKFLOW_CONFIGFILE_KEYWORD,
                SnakemakeNames.WORKFLOW_INCLUDE_KEYWORD,
                SnakemakeNames.WORKFLOW_REPORT_KEYWORD
        )
    }

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SMKElementVisitor -> pyVisitor.visitSMKWorkflowParameterListStatement(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }

    private fun createReference(textRange: TextRange) =
            when (keywordName) {
                SnakemakeNames.WORKFLOW_CONFIGFILE_KEYWORD -> SmkConfigfileReference(this, textRange)
                SnakemakeNames.WORKFLOW_REPORT_KEYWORD -> SmkReportReference(this, textRange)
                else -> SmkIncludeReference(this, textRange)
            } as PsiReference

    override fun getReferences(): Array<PsiReference> {
        if (keywordName !in WORKFLOWS_WITH_FILE_REFERENCES) {
            return emptyArray()
        }

        val stringLiteralArgs = this.argumentList?.arguments?.filter {
            it is PyStringLiteralExpression
        }

        return stringLiteralArgs?.map {
            val offset = keywordName!!.length + it.startOffsetInParent
            createReference(TextRange(offset + 1, offset + it.textLength - 1))
        }?.toTypedArray() ?: emptyArray()
    }

    private val keywordName: String?
            get() = getKeywordNode()?.text

    val argumentList: PyArgumentList?
        get() = children.firstOrNull { it is PyArgumentList } as PyArgumentList?

    fun getKeywordNode() = node.findChildByType(SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_PARAMLISTS_DECORATOR_KEYWORDS)
}