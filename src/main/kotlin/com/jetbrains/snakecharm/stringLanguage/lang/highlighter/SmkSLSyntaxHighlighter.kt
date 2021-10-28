package com.jetbrains.snakecharm.stringLanguage.lang.highlighter

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.snakecharm.stringLanguage.lang.parser.SmkSLLexerAdapter
import com.jetbrains.snakecharm.stringLanguage.lang.parser.SmkSLTokenTypes

class SmkSLSyntaxHighlighter : SyntaxHighlighterBase() {
    companion object {
        val BRACES = createTextAttributesKey("SMKSL_BRACES", PyHighlighter.PY_FSTRING_FRAGMENT_BRACES)
        val COMMA = createTextAttributesKey("SMKSL_COMMA", PyHighlighter.PY_FSTRING_FRAGMENT_COLON)
        val STRING_CONTENT = createTextAttributesKey("SMKSL_STRING_CONTENT", PyHighlighter.PY_BYTE_STRING)
        val FORMAT_SPECIFIER = createTextAttributesKey("SMKSL_FORMAT_SPECIFIER", PyHighlighter.PY_NUMBER)
        val ACCESS_KEY = createTextAttributesKey("SMKSL_ACCESS_KEY", PyHighlighter.PY_KEYWORD_ARGUMENT)
        val IDENTIFIER = createTextAttributesKey("SMKSL_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER)
        val HIGHLIGHTING_WILDCARDS_KEY =
            createTextAttributesKey("SMKSL_WILDCARD", DefaultLanguageHighlighterColors.NUMBER)
    }

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        when {
            tokenType === SmkSLTokenTypes.LBRACE ||
                    tokenType === SmkSLTokenTypes.RBRACE -> arrayOf(BRACES)
            tokenType === SmkSLTokenTypes.COMMA -> arrayOf(COMMA)
            tokenType === SmkSLTokenTypes.STRING_CONTENT -> arrayOf(STRING_CONTENT)
            tokenType === SmkSLTokenTypes.FORMAT_SPECIFIER -> arrayOf(FORMAT_SPECIFIER)
            tokenType === SmkSLTokenTypes.ACCESS_KEY -> arrayOf(ACCESS_KEY)
            tokenType === SmkSLTokenTypes.IDENTIFIER -> arrayOf(IDENTIFIER)
            else -> emptyArray()
        }

    override fun getHighlightingLexer() = SmkSLLexerAdapter()
}
