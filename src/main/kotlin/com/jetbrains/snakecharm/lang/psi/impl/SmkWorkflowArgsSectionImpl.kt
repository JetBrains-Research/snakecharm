package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import com.intellij.util.ArrayUtil
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.codeInsight.SnakemakeApi.WORKFLOW_SECTIONS_WITH_FILE_REFERENCES
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes
import com.jetbrains.snakecharm.lang.psi.*

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */
class SmkWorkflowArgsSectionImpl(node: ASTNode) : PyElementImpl(node), SmkWorkflowArgsSection {

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkElementVisitor -> pyVisitor.visitSmkWorkflowArgsSection(this)
        else -> super<PyElementImpl>.acceptPyVisitor(pyVisitor)
    }

    override fun getSectionKeywordNode() = node
        .findChildByType(SmkTokenTypes.WORKFLOW_TOPLEVEL_ARGS_SECTION_KEYWORD)

    override fun getPresentation() = getPresentation(this)
    override fun getIcon(flags: Int) = getIcon(this, flags)

    private fun createReference(strExpr: PyStringLiteralExpression): SmkFileReference {
        val offsetInParent = keywordName!!.length + strExpr.startOffsetInParent
        val textRange = SmkPsiUtil.getReferenceRange(strExpr).shiftRight(offsetInParent)
        val path = strExpr.stringValue

        return when (keywordName) {
            SnakemakeNames.WORKFLOW_CONFIGFILE_KEYWORD -> SmkConfigfileReference(
                this, textRange, strExpr, path
            )
            SnakemakeNames.WORKFLOW_PEPFILE_KEYWORD -> SmkPepfileReference(
                this, textRange, strExpr, path
            )
            SnakemakeNames.WORKFLOW_PEPSCHEMA_KEYWORD -> SmkPepschemaReference(
                this, textRange, strExpr, path
            )
            SnakemakeNames.WORKFLOW_REPORT_KEYWORD -> SmkReportReference(
                this, textRange, strExpr, path
            )
            SnakemakeNames.WORKFLOW_WORKDIR_KEYWORD -> SmkWorkDirReference(
                this, textRange, strExpr, path
            )
            SnakemakeNames.WORKFLOW_CONDA_KEYWORD -> SmkCondaEnvReference(
                this, textRange, strExpr, path
            )
            else -> SmkIncludeReference(
                this, textRange, strExpr, path
            )
        }
    }

    override fun getReference(): PsiReference? = ArrayUtil.getFirstElement(this.references)

    override fun getReferences(): Array<PsiReference> {
        if (keywordName !in WORKFLOW_SECTIONS_WITH_FILE_REFERENCES) {
            return emptyArray()
        }

        val stringLiteralArgs =
            argumentList?.arguments?.filterIsInstance<PyStringLiteralExpression>() ?: return emptyArray()

        return stringLiteralArgs.map { createReference(it) }.toTypedArray()
    }

    private val keywordName: String?
        get() = getKeywordNode()?.text

    private fun getKeywordNode() = node.findChildByType(SmkTokenTypes.WORKFLOW_TOPLEVEL_ARGS_SECTION_KEYWORD)
}