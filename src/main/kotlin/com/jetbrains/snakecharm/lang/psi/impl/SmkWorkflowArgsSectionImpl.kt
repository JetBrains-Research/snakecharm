package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes
import com.jetbrains.snakecharm.lang.psi.*

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */
class SmkWorkflowArgsSectionImpl(node: ASTNode) : PyElementImpl(node), SmkWorkflowArgsSection {
    companion object {
        val WORKFLOWS_WITH_FILE_REFERENCES = setOf(
                SnakemakeNames.WORKFLOW_CONFIGFILE_KEYWORD,
                SnakemakeNames.WORKFLOW_INCLUDE_KEYWORD,
                SnakemakeNames.WORKFLOW_REPORT_KEYWORD,
                SnakemakeNames.WORKFLOW_WORKDIR_KEYWORD
        )
    }

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkElementVisitor -> pyVisitor.visitSmkWorkflowArgsSection(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }

    override fun getSectionKeywordNode()= node
            .findChildByType(SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_PARAMLISTS_DECORATOR_KEYWORDS)

    override fun getPresentation() = super<SmkWorkflowArgsSection>.getPresentation()
    override fun getIcon(flags: Int) = super<SmkWorkflowArgsSection>.getIcon(flags)

    private fun createReference(textRange: TextRange, path: String) =
            when (keywordName) {
                SnakemakeNames.WORKFLOW_CONFIGFILE_KEYWORD -> SmkConfigfileReference(this, textRange, path)
                SnakemakeNames.WORKFLOW_REPORT_KEYWORD -> SmkReportReference(this, textRange, path)
                SnakemakeNames.WORKFLOW_WORKDIR_KEYWORD -> SmkWorkDirReference(this, textRange, path)
                else -> SmkIncludeReference(this, textRange, path)
            }

    override fun getReferences(): Array<PsiReference> {
        if (keywordName !in WORKFLOWS_WITH_FILE_REFERENCES) {
            return emptyArray()
        }

        val stringLiteralArgs = argumentList?.arguments?.filter {
            it is PyStringLiteralExpression
        } ?: return emptyArray()

        return stringLiteralArgs.map {
            val path = (it as PyStringLiteralExpression).stringValue
            val offset = keywordName!!.length + it.startOffsetInParent + it.text.indexOf(path)
            createReference(TextRange(offset, offset + path.length), path)
        }.toTypedArray()
    }

    private val keywordName: String?
        get() = getKeywordNode()?.text

    private fun getKeywordNode() = node.findChildByType(SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_PARAMLISTS_DECORATOR_KEYWORDS)
}


class SmkWorkflowArgsSectionManipulator : AbstractElementManipulator<SmkWorkflowArgsSectionImpl>() {
    override fun handleContentChange(
            element: SmkWorkflowArgsSectionImpl,
            range: TextRange,
            newContent: String): SmkWorkflowArgsSectionImpl? {
        val replacedText = element.findElementAt(range.startOffset) ?: return element

        val stringLiteral =  PsiTreeUtil.getParentOfType(replacedText, PyStringLiteralExpression::class.java)!!
        val elementGenerator = PyElementGenerator.getInstance(element.project)
        val newStringLiteral = elementGenerator.createStringLiteral(stringLiteral, newContent)

        stringLiteral.replace(newStringLiteral)

        return element
    }
}