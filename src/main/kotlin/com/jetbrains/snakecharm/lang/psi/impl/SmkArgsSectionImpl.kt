package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.psi.SmkArgsSection
import com.jetbrains.snakecharm.lang.psi.getIcon
import com.jetbrains.snakecharm.lang.psi.getPresentation
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil.getIdentifierNode

abstract class SmkArgsSectionImpl(node: ASTNode): PyElementImpl(node), SmkArgsSection {
    override fun getName() = getSectionKeywordNode()?.text

    override fun getSectionKeywordNode() = getIdentifierNode(node)

    override fun getPresentation() = getPresentation(this)
    override fun getIcon(flags: Int) = getIcon(this, flags)
}