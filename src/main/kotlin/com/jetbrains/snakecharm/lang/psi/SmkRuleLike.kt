package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.TokenType
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.codeInsight.controlflow.ScopeOwner
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.PyStatementList
import com.jetbrains.python.psi.PyStatementListContainer
import com.jetbrains.python.psi.PyUtil
import com.jetbrains.python.psi.impl.PyElementImpl
import com.jetbrains.python.psi.impl.PyPsiUtils
import com.jetbrains.snakecharm.SnakemakeIcons
import javax.swing.Icon

abstract class SmkRuleLike<out T:SmkSectionStatement>(node: ASTNode): PyElementImpl(node),
        PyStatementListContainer, PyStatement, ScopeOwner,
        PsiNamedElement, PsiNameIdentifierOwner
{
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
        val newNameNode = PyUtil.createNewName(this, name)
        getNameNode()?.let {
            node.replaceChild(it, newNameNode)
        }
        return this
    }

    override fun getNameIdentifier() = getNameNode()?.psi

    /**
     * Use name start offset here, required for navigation & find usages, e.g. when ask for usages on name identifier
     */
    override fun getTextOffset() = getNameNode()?.startOffset ?: super.getTextOffset()

    private fun getNameNode() = getIdentifierNode(node)

    fun getSectionByName(sectionName: String) =
            statementList.statements.find {
                (it as SmkSectionStatement).section.textMatches(sectionName)
            } as? T

    override fun getStatementList() = childToPsiNotNull<PyStatementList>(PyElementTypes.STATEMENT_LIST)

    // iterate over children, not statements, since SMKRuleRunParameter isn't a statement
    fun getSections() = statementList.children.filterIsInstance<SMKRuleSection>()

    override fun getIcon(flags: Int): Icon? {
        PyPsiUtils.assertValid(this)
        return SnakemakeIcons.FILE
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