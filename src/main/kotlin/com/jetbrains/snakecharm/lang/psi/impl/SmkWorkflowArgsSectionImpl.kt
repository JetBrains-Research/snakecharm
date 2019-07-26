package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkWorkflowArgsSection
import com.jetbrains.snakecharm.lang.psi.getIcon
import com.jetbrains.snakecharm.lang.psi.getPresentation

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */
class SmkWorkflowArgsSectionImpl(node: ASTNode) : PyElementImpl(node), SmkWorkflowArgsSection {
    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkElementVisitor -> pyVisitor.visitSmkWorkflowArgsSection(this)
        else -> super.acceptPyVisitor(pyVisitor)
    }

    override fun getSectionKeywordNode()= node
            .findChildByType(SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_PARAMLISTS_DECORATOR_KEYWORDS)

    override fun getPresentation() = getPresentation(this)
    override fun getIcon(flags: Int) = getIcon(this, flags)
}