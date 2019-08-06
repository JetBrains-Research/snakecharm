package com.jetbrains.snakecharm.string_language.lang.highlighter

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.jetbrains.snakecharm.string_language.SmkSLTokenTypes
import com.jetbrains.snakecharm.string_language.lang.parser.SmkSLLexerAdapter

class SmkSLHighlighter : SyntaxHighlighterBase() {
    companion object {
        val BRACES = arrayOf(createTextAttributesKey("BRACES", DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE))
        val COMMA = arrayOf(createTextAttributesKey("COMMA", DefaultLanguageHighlighterColors.MARKUP_ATTRIBUTE))
        val STRING_CONTENT = arrayOf(createTextAttributesKey("STRING_CONTENT", DefaultLanguageHighlighterColors.STRING))
        val ACCESS_KEY =  arrayOf(createTextAttributesKey("ACCESS_KEY", DefaultLanguageHighlighterColors.NUMBER))
    }

    override fun getTokenHighlights(tokenType: IElementType?) =
        when {
            tokenType === SmkSLTokenTypes.LBRACE ||
                    tokenType === SmkSLTokenTypes.RBRACE -> BRACES
            tokenType === SmkSLTokenTypes.COMMA -> COMMA
            tokenType === SmkSLTokenTypes.STRING_CONTENT -> STRING_CONTENT
            tokenType === SmkSLTokenTypes.ACCESS_KEY -> ACCESS_KEY
            else -> emptyArray()
        }

    override fun getHighlightingLexer() = SmkSLLexerAdapter()
}