package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyArgumentList
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.impl.PyElementImpl

abstract class SmkSectionStatement(node: ASTNode): PyElementImpl(node), PyStatement, SMKRuleSection {
    override fun getName() = getNameNode()?.text

    open fun getNameNode() = getIdentifierNode(node)

    override val sectionName: String?
        get() = getNameNode()?.text

    open val section: PsiElement?
        get() = getNameNode()?.psi

    open val keywordArguments: List<PyKeywordArgument>?
        get() = argumentList?.arguments?.filterIsInstance<PyKeywordArgument>()

    open val argumentList: PyArgumentList?
        get() = children.firstOrNull { it is PyArgumentList } as PyArgumentList?
}