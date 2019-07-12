package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.impl.PyElementImpl

interface SmkArgsSection: SmkSection {
    val argumentList: PyArgumentList?
        get() = children.firstOrNull { it is PyArgumentList } as PyArgumentList?

    val keywordArguments: List<PyKeywordArgument>?
        get() = argumentList?.arguments?.filterIsInstance<PyKeywordArgument>()
}

abstract class SmkArgsSectionImpl(node: ASTNode): PyElementImpl(node), SmkArgsSection {
    override fun getName() = getSectionKeywordNode()?.text

    override fun getSectionKeywordNode() = getIdentifierNode(node)
}