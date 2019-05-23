package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.TokenType
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.psi.PyElementVisitor
import com.jetbrains.python.psi.PyStatementList
import com.jetbrains.python.psi.PyStatementListContainer
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.snakecharm.inspections.SnakemakeInspectionVisitor
import com.jetbrains.snakecharm.lang.validation.SnakemakeAnnotator

open class SMKRule(node: ASTNode): PyElementImpl(node), PyStatementListContainer, PsiNamedElement {
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

    override fun acceptPyVisitor(pyVisitor: PyElementVisitor) {
        when (pyVisitor) {
            is SnakemakeAnnotator -> pyVisitor.visitSMKRule(this)
            is SnakemakeInspectionVisitor -> pyVisitor.visitSMKRule(this)
            else -> super.acceptPyVisitor(pyVisitor)
        }
    }

    fun getSections(): List<SMKRuleSection> {
        val sections = mutableListOf<SMKRuleSection>()
        // iterate over children, not statements, since SMKRuleRunParameter isn't a statement
        statementList.children.forEach {
            if (it is SMKRuleSection) {
                sections.add(it)
            }
        }

        return sections
    }

    override fun getStatementList() = childToPsiNotNull<PyStatementList>(PyElementTypes.STATEMENT_LIST)

    fun getSections(): List<SMKRuleSection> {
        val sections = mutableListOf<SMKRuleSection>()
        // iterate over children, not statements, since SMKRuleRunParameter isn't a statement
        statementList.children.forEach {
            if (it is SMKRuleSection) {
                sections.add(it)
            }
        }

        return sections
    }
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