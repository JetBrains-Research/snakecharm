package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes
import com.jetbrains.snakecharm.lang.psi.SmkElementVisitor
import com.jetbrains.snakecharm.lang.psi.SmkWorkflowLocalrulesSection
import com.jetbrains.snakecharm.lang.psi.getIcon
import com.jetbrains.snakecharm.lang.psi.getPresentation

class SmkWorkflowLocalrulesSectionImpl(node: ASTNode): PyElementImpl(node), SmkWorkflowLocalrulesSection {
    override fun getSectionKeywordNode() = node.findChildByType(SmkTokenTypes.WORKFLOW_LOCALRULES_KEYWORD)

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) = when (pyVisitor) {
        is SmkElementVisitor -> pyVisitor.visitSmkWorkflowLocalrulesSection(this)
        else -> super<PyElementImpl>.acceptPyVisitor(pyVisitor)
    }

    override fun getPresentation() = getPresentation(this)
    override fun getIcon(flags: Int) = getIcon(this, flags)
}