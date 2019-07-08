package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyReferenceOwner
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.RatedResolveResult

class SMKRuleReference(node: ASTNode): PyElementImpl(node), PsiNamedElement{
    override fun getName() = getNameNode()?.text

    override fun setName(name: String): PsiElement {
        val nameElement = PyUtil.createNewName(this, name)
        val nameNode = getNameNode()
        if (nameNode != null) {
            node.replaceChild(nameNode, nameElement)
        }
        return this
    }

    override fun getReference(): PsiReference? = SMKRulePSIReference(this, TextRange(0, textLength))

    private fun getNameNode() = getIdentifierNode(node)

    private class SMKRulePSIReference(
            element: PsiElement,
            textRange: TextRange
    ) : PsiReferenceBase<PsiElement>(element, textRange) {
        private val key: String = element.text

        override fun resolve(): PsiElement? =
                getRules().firstOrNull { it.first == key }?.second ?:
                getCheckpoints().firstOrNull { it.first == key }?.second

        private fun getRules() = PsiTreeUtil.getParentOfType(element, SnakemakeFile::class.java)
                ?.collectRules() ?: emptyList()

        private fun getCheckpoints() = PsiTreeUtil.getParentOfType(element, SnakemakeFile::class.java)
                ?.collectCheckPoints()
                ?: emptyList()
    }
}

