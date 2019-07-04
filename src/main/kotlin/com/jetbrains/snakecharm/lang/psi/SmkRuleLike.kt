package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.TokenType
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.psi.PyStatementList
import com.jetbrains.python.psi.PyStatementListContainer
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.impl.PyElementImpl

abstract class SmkRuleLike(node: ASTNode): PyElementImpl(node), PyStatementListContainer, PsiNamedElement {
    //TODO: PyNamedElementContainer; PyStubElementType<SMKRuleStub, SMKRule>
    // SnakemakeNamedElement, SnakemakeScopeOwner

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

    fun getSectionByName(sectionName: String) =
            statementList.statements.find {
                (it as SmkSectionStatement).section.textMatches(sectionName)
            } as? SmkSectionStatement

    override fun getStatementList() = childToPsiNotNull<PyStatementList>(PyElementTypes.STATEMENT_LIST)

    // iterate over children, not statements, since SMKRuleRunParameter isn't a statement
    fun getSections() = statementList.children.filterIsInstance<SMKRuleSection>()
}

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