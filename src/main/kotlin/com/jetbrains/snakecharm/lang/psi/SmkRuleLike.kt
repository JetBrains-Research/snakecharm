package com.jetbrains.snakecharm.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.TokenType
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyStatement
import com.jetbrains.python.psi.PyStatementListContainer

interface SmkRuleLike<out S : SmkSection>: PyElement, PyStatementListContainer, PyStatement,
        //ScopeOwner,
        PsiNamedElement, PsiNameIdentifierOwner {

    fun getSections(): List<SmkSection>
    fun getSectionByName(sectionName: String): S?
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