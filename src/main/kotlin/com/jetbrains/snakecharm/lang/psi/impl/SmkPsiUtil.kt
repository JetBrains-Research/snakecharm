package com.jetbrains.snakecharm.lang.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect
import com.jetbrains.snakecharm.string_language.SmkSL

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

    fun isInsideFileWithLanguage(foothold: PsiElement?, language: Language): Boolean {
        if (foothold == null) {
            return false
        }
        return foothold.isValid && foothold.containingFile?.language === language
    }

    fun isInsideSnakemakeOrSmkSLFile(foothold: PsiElement?) =
            isInsideFileWithLanguage(foothold, SnakemakeLanguageDialect) ||
                    isInsideFileWithLanguage(foothold, SmkSL)
}