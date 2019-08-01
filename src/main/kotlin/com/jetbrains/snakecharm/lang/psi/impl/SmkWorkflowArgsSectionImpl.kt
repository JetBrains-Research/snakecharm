package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
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

    override fun getPresentation() = getPresentation(this)
    override fun getIcon(flags: Int) = getIcon(this, flags)

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

        val stringLiteralArgs = argumentList?.arguments?.filterIsInstance<PyStringLiteralExpression>()
                ?: return emptyArray()

        return stringLiteralArgs.map {
            val path = it.stringValue
            val offsetInParent = keywordName!!.length + it.startOffsetInParent
            createReference(getReferenceRange(it).shiftRight(offsetInParent), path)
        }.toTypedArray()
    }

    // This function is meant to get correct reference range in string literal
    // even in some weird cases like:
    // include: "f" "o" "o" ".s" "m" """k"""
    private fun getReferenceRange(stringLiteral: PyStringLiteralExpression): TextRange {
        val decodedFragments = stringLiteral.decodedFragments
        return TextRange(decodedFragments.first().first.startOffset, decodedFragments.last().first.endOffset)
    }

    private val keywordName: String?
        get() = getKeywordNode()?.text

    private fun getKeywordNode() = node.findChildByType(SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_PARAMLISTS_DECORATOR_KEYWORDS)
}