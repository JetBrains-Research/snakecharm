package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.PythonDialectsTokenSetProvider

object SmkPsiUtil {
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