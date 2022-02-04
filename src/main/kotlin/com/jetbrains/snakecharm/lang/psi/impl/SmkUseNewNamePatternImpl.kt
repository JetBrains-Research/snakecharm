package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.psi.SmkUseNewNamePattern

class SmkUseNewNamePatternImpl(node: ASTNode) : PyElementImpl(node), SmkUseNewNamePattern {
    override fun isWildcard(): Boolean = text.contains('*')

    override fun getNameBeforeWildcard(): PsiElement = firstChild
}