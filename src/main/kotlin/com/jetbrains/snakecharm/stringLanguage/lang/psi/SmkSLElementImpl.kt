package com.jetbrains.snakecharm.stringLanguage.lang.psi

import com.intellij.lang.ASTNode
import com.jetbrains.python.psi.impl.PyElementImpl

open class SmkSLElementImpl(node: ASTNode) : PyElementImpl(node), SmkSLElement {
    override fun toString() = "${this::class.java.simpleName}(${node.elementType})"
}