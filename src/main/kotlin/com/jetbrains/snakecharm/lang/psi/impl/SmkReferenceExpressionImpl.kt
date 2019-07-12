package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.python.psi.resolve.RatedResolveResult
import com.jetbrains.snakecharm.lang.psi.SmkReferenceExpression
import com.jetbrains.snakecharm.lang.psi.SmkRuleOrCheckpoint
import com.jetbrains.snakecharm.lang.psi.SmkFile
import com.jetbrains.snakecharm.lang.psi.impl.SmkPsiUtil.getIdentifierNode
import com.jetbrains.snakecharm.lang.psi.types.AbstractSmkRuleOrCheckpointType

class SmkReferenceExpressionImpl(node: ASTNode): PyElementImpl(node), SmkReferenceExpression {
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
            element: PsiNamedElement,
            textRange: TextRange
    ) : PsiReferenceBase<PsiNamedElement>(element, textRange), PsiPolyVariantReference {
        private val key: String = element.text

        override fun resolve(): PsiElement? =
                multiResolve(false).firstOrNull()?.element

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
                (getRules() + getCheckpoints())
                        .filter { (name, _) -> name == key }
                        .map { (_, psi) -> RatedResolveResult(RatedResolveResult.RATE_NORMAL, psi) }
                        .toTypedArray()

        override fun getVariants(): Array<Any> =
                (getRules() + getCheckpoints()).map { (name, elem) ->
                    AbstractSmkRuleOrCheckpointType.createRuleLikeLookupItem(name, elem as SmkRuleOrCheckpoint)
                }.toTypedArray()

        override fun handleElementRename(newElementName: String): PsiElement =
                element.setName(newElementName)

        private fun getRules() = PsiTreeUtil.getParentOfType(element, SmkFile::class.java)
                ?.collectRules() ?: emptyList()

        private fun getCheckpoints() = PsiTreeUtil.getParentOfType(element, SmkFile::class.java)
                ?.collectCheckPoints()
                ?: emptyList()
    }
}

