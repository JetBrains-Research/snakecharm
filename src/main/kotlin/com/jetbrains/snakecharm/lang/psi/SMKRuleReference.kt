package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import com.jetbrains.python.psi.resolve.PyResolveContext

class SMKRuleReference(node: ASTNode): PyElementImpl(node), PsiNamedElement {
    override fun getName() = getNameNode()?.text

    override fun setName(name: String): PsiElement {
        val nameElement = PyUtil.createNewName(this, name)
        val nameNode = getNameNode()
        if (nameNode != null) {
            node.replaceChild(nameNode, nameElement)
        }
        return this
    }

    private fun getNameNode() = getIdentifierNode(node)
}