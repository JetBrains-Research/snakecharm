package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.TokenType
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.lang.validation.SnakemakeAnnotator

open class SMKRule(node: ASTNode): PyElementImpl(node), PsiNamedElement { //TODO: PyNamedElementContainer; PyStubElementType<SMKRuleStub, SMKRule>
    // SnakemeakeNamedElement, SnakemakeScopeOwner

    companion object {
        val PARAMS_KEYWORDS = SMKRuleParameterListStatement.PARAMS_NAMES + SMKRuleRunParameter.PARAM_NAME

        fun getIdentifierNode(node: ASTNode): ASTNode? {
            var id = node.findChildByType(PyTokenTypes.IDENTIFIER)
            if (id == null) {
                val error = node.findChildByType(TokenType.ERROR_ELEMENT)
                if (error != null) {
                    // TODO: do we need this? it is like in PyFunction
                    id = error.findChildByType(PythonDialectsTokenSetProvider.INSTANCE.keywordTokens)
                }
            }
            return id
        }
    }

    override fun getName(): String? {
//        val stub = stub
//        if (stub != null) {
//            return stub.getName()
//        }

        return getNameNode()?.text
    }

    override fun setName(name: String): PsiElement {
        val nameElement = PyUtil.createNewName(this, name)
        val nameNode = getNameNode()
        if (nameNode != null) {
            node.replaceChild(nameNode, nameElement)
        }
        return this
    }

    fun getNameNode() = getIdentifierNode(node)

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) {
        if (pyVisitor is SnakemakeAnnotator) {
            pyVisitor.visitSMKRule(this)
        } else {
            super.acceptPyVisitor(pyVisitor)
        }
    }
}