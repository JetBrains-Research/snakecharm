package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.TokenType
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.psi.PyStringLiteralExpression

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

    // This function is meant to get correct reference range in string literal
    // even in some weird cases like:
    // include: "f" "o" "o" ".s" "m" """k"""
    fun getReferenceRange(stringLiteral: PyStringLiteralExpression): TextRange {
        val decodedFragments = stringLiteral.decodedFragments

        // It can happen for example in this case:
        // include: f""
        if (decodedFragments.isEmpty()) {
            return TextRange.EMPTY_RANGE
        }

        return TextRange(decodedFragments.first().first.startOffset, decodedFragments.last().first.endOffset)
    }
}