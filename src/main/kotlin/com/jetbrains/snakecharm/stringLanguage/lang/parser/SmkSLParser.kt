package com.jetbrains.snakecharm.stringLanguage.lang.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.stringLanguage.SmkSLTokenTypes

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

        builder.checkMatches(SmkSLTokenTypes.LBRACE,
                SnakemakeBundle.message("SMKSL.PARSE.expected.lbrace"))

        val doParseRegExp = parseIdentifierExpr(builder)
        // Parsing regex
        if (doParseRegExp) {
            builder.checkMatches(SmkSLTokenTypes.COMMA,
                    SnakemakeBundle.message("SMKSL.PARSE.expected.comma"))
            builder.checkMatches(SmkSLTokenTypes.REGEXP,
                    SnakemakeBundle.message("SMKSL.PARSE.expected.regexp"))

            // RegExp will be parsed automatically, because its token
            // type extends ILazyParseableElementType
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
        fun PsiBuilder.Marker.drop(identifierExpected: Boolean) {
            if (identifierExpected) {
                builder.error(SnakemakeBundle.message("SMKSL.PARSE.expected.identifier.name"))
            }
            drop()
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
                tt === SmkSLTokenTypes.BAD_CHARACTER -> {
                    exprMarker.drop()

                    val errorMarker = builder.mark()
                    builder.advanceLexer()
                    errorMarker.error(SnakemakeBundle.message("SMKSL.PARSE.unexpected.character"))

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
                tt === SmkSLTokenTypes.FORMAT_SPECIFIER -> {
                    builder.advanceLexer()
                    exprMarker.done(SmkSLTokenTypes.FORMAT_SPECIFIER_EXPRESSION)
                    exprMarker = exprMarker.precede()
                }
                tt === SmkSLTokenTypes.LBRACKET -> {
                    builder.advanceLexer()

                    val keyMarker = builder.mark()
                    builder.checkMatches(SmkSLTokenTypes.IDENTIFIER,
                            SnakemakeBundle.message("SMKSL.PARSE.expected.key"))
                    keyMarker.done(SmkSLTokenTypes.KEY_EXPRESSION)

                    builder.checkMatches(SmkSLTokenTypes.RBRACKET,
                            SnakemakeBundle.message("SMKSL.PARSE.expected.rbracket"))
                    exprMarker.done(SmkSLTokenTypes.SUBSCRIPTION_EXPRESSION)

                    exprMarker = exprMarker.precede()
                }
                tt === SmkSLTokenTypes.COMMA -> {
                    exprMarker.drop(identifierExpected)
                    return true
                }
                // TODO: better errors handling
                else -> {
                    exprMarker.drop(identifierExpected)
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
