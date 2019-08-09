package com.jetbrains.snakecharm.string_language.lang.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import com.jetbrains.snakecharm.SnakemakeBundle
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
            builder.checkMatches(SmkSLTokenTypes.REGEXP,
                    SnakemakeBundle.message("SMKSL.PARSE.expected.regexp"))
            regexMarker.done(SmkSLTokenTypes.REGEXP)
        }

        // Skipping '}'
        if (!builder.eof()) {
            builder.advanceLexer()
        } else {
            builder.error(SnakemakeBundle.message("SMKSL.PARSE.expected.rbrace"))
        }

        languageMarker.done(SmkSLTokenTypes.LANGUAGE)
    }

    // Returns true if parsing ended on token 'COMMA', and
    // regexp has to be parsed
    private fun parseIdentifierExpr(builder: PsiBuilder): Boolean {
        fun endParsing(marker: PsiBuilder.Marker, identifierExpected: Boolean) {
            if (identifierExpected) {
                builder.error(SnakemakeBundle.message("SMKSL.PARSE.expected.identifier.name"))
            }
            marker.drop()
        }

        var exprMarker = builder.mark()
        var identifierExpected = true
        while(true) {
            val tt = builder.tokenType
            when {
                tt === SmkSLTokenTypes.IDENTIFIER -> {
                    builder.advanceLexer()
                    exprMarker.done(SmkSLTokenTypes.REFERENCE_EXPRESSION)
                    exprMarker = exprMarker.precede()
                    identifierExpected = false
                }
                tt === SmkSLTokenTypes.UNEXPECTED_TOKEN -> {
                    exprMarker.done(SmkSLTokenTypes.REFERENCE_EXPRESSION)

                    exprMarker = builder.mark()
                    builder.advanceLexer()
                    exprMarker.error(SnakemakeBundle.message("SMKSL.PARSE.unexpected.character"))

                    exprMarker = builder.mark()
                }
                tt === SmkSLTokenTypes.DOT -> {
                    builder.advanceLexer()
                    if (builder.tokenType === SmkSLTokenTypes.IDENTIFIER) {
                        builder.advanceLexer()
                    } else {
                        identifierExpected = true
                    }
                    exprMarker.done(SmkSLTokenTypes.REFERENCE_EXPRESSION)
                    exprMarker = exprMarker.precede()
                }
                tt === SmkSLTokenTypes.LBRACKET -> {
                    builder.advanceLexer()

                    val marker = builder.mark()
                    builder.checkMatches(SmkSLTokenTypes.ACCESS_KEY,
                            SnakemakeBundle.message("SMKSL.PARSE.expected.key"))
                    marker.done(SmkSLTokenTypes.ACCESS_KEY)

                    builder.checkMatches(SmkSLTokenTypes.RBRACKET,
                            SnakemakeBundle.message("SMKSL.PARSE.expected.rbracket"))
                    exprMarker.done(SmkSLTokenTypes.SUBSCRIPTION_EXPRESSION)

                    exprMarker = exprMarker.precede()
                }
                tt === SmkSLTokenTypes.COMMA -> {
                    endParsing(exprMarker, identifierExpected)
                    return true
                }
                else -> {
                    endParsing(exprMarker, identifierExpected)
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
