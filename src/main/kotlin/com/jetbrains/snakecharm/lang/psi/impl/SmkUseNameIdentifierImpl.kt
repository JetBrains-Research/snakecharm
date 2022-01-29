package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.psi.SmkUseNameIdentifier

class SmkUseNameIdentifierImpl(node: ASTNode) : PyElementImpl(node), SmkUseNameIdentifier {
    override fun isWildcard(): Boolean = text.contains('*')

    override fun getNameBeforeWildcard(): PsiElement = firstChild
}