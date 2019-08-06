package com.jetbrains.snakecharm.string_language.lang.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import com.jetbrains.snakecharm.string_language.SmkSLTokenTypes

class SmkSLParser : PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()

        while (builder.tokenType != null) {
            if (builder.tokenType === SmkSLTokenTypes.STRING_CONTENT) {
                val stringMarker = builder.mark()
                builder.advanceLexer()
                stringMarker.done(SmkSLTokenTypes.STRING_CONTENT)
            } else {
                parseLanguage(builder)
            }
        }

        rootMarker.done(root)
        return builder.treeBuilt
    }

    private fun parseLanguage(builder: PsiBuilder) {
        val languageMarker = builder.mark()

        // Skipping '{'
        builder.advanceLexer()

        val expressionMarker = builder.mark()
        val doParseRegExp = parseIdentifierExpr(builder)
        expressionMarker.done(SmkSLTokenTypes.EXPRESSION_STATEMENT)

        // Parsing regex
        if (doParseRegExp) {
            // Skipping ','
            builder.advanceLexer()

            val regexMarker = builder.mark()
            builder.checkMatches(SmkSLTokenTypes.REGEXP, "Expected regular expression")
            regexMarker.done(SmkSLTokenTypes.REGEXP)
        }

        // Skipping '}'
        if (!builder.eof()) {
            builder.advanceLexer()
        } else {
            builder.error("Expected '}'")
        }

        languageMarker.done(SmkSLTokenTypes.LANGUAGE)
    }

    // Returns true if parsing ended on token 'COMMA', and
    // regexp has to be parsed
    private fun parseIdentifierExpr(builder: PsiBuilder): Boolean {
        var exprMarker = builder.mark()

        // Parse first identifier
        builder.checkMatches(SmkSLTokenTypes.IDENTIFIER, "Expected identifier name")
        exprMarker.done(SmkSLTokenTypes.REFERENCE_EXPRESSION)
        exprMarker = exprMarker.precede()

        while(true) {
            val tt = builder.tokenType
            when {
                tt === SmkSLTokenTypes.UNEXPECTED_TOKEN ||
                tt === SmkSLTokenTypes.DOT -> {
                    builder.advanceLexer()

                    builder.checkMatches(SmkSLTokenTypes.IDENTIFIER, "Expected identifier name")
                    exprMarker.done(SmkSLTokenTypes.REFERENCE_EXPRESSION)
                    exprMarker = exprMarker.precede()
                }
                tt === SmkSLTokenTypes.LBRACKET -> {
                    builder.advanceLexer()

                    val marker = builder.mark()
                    builder.checkMatches(SmkSLTokenTypes.ACCESS_KEY, "Expected key")
                    marker.done(SmkSLTokenTypes.ACCESS_KEY)

                    builder.checkMatches(SmkSLTokenTypes.RBRACKET, "Expected ']'")
                    exprMarker.done(SmkSLTokenTypes.SUBSCRIPTION_EXPRESSION)

                    exprMarker = exprMarker.precede()
                }
                tt === SmkSLTokenTypes.COMMA -> {
                    exprMarker.drop()
                    return true
                }
                else -> {
                    exprMarker.drop()
                    return false
                }
            }
        }
    }

    private fun PsiBuilder.checkMatches(token: IElementType, message: String): Boolean {
        if (tokenType === token) {
            advanceLexer()
            return true
        }

        error(message)
        return false
    }
}
