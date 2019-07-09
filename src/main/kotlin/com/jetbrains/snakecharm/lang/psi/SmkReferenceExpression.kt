package com.jetbrains.snakecharm.lang.psi

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.impl.PyElementImpl

class SmkReferenceExpression(node: ASTNode): PyElementImpl(node), PsiNamedElement{
    override fun getName() = getNameNode()?.text

    override fun setName(name: String): PsiElement {
        val nameElement = PyUtil.createNewName(this, name)
        val nameNode = getNameNode()
        if (nameNode != null) {
            node.replaceChild(nameNode, nameElement)
        }
        return this
    }

    override fun getReference(): PsiReference? = SmkRuleOrCheckpointNameReference(this, TextRange(0, textLength))

    private fun getNameNode() = getIdentifierNode(node)

    private class SmkRuleOrCheckpointNameReference(
            element: PsiElement,
            textRange: TextRange
    ) : PsiReferenceBase<PsiElement>(element, textRange) {
        private val key: String = element.text

        override fun resolve(): PsiElement? =
                getRules().firstOrNull { it.first == key }?.second ?:
                getCheckpoints().firstOrNull { it.first == key }?.second

        override fun getVariants(): Array<Any> {
            val variants = mutableListOf<LookupElement>()
            variants.addAll(getRules().map {
                LookupElementBuilder.create(it.first)
                        .withTypeText(element.containingFile.name)
            })
            variants.addAll(getCheckpoints().map {
                LookupElementBuilder.create(it.first)
                        .withTypeText(element.containingFile.name)
            })
            return variants.toTypedArray()
        }

        private fun getRules() = PsiTreeUtil.getParentOfType(element, SnakemakeFile::class.java)
                ?.collectRules() ?: emptyList()

        private fun getCheckpoints() = PsiTreeUtil.getParentOfType(element, SnakemakeFile::class.java)
                ?.collectCheckPoints()
                ?: emptyList()
    }
}

