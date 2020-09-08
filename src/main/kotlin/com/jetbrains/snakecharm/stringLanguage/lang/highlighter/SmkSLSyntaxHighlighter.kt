package com.jetbrains.snakecharm.stringLanguage.lang.highlighter

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.snakecharm.stringLanguage.lang.parser.SmkSLLexerAdapter
import com.jetbrains.snakecharm.stringLanguage.lang.parser.SmkSLTokenTypes

class SmkSLSyntaxHighlighter : SyntaxHighlighterBase() {
    companion object {
        val BRACES = arrayOf(createTextAttributesKey("SMKSL_BRACES", PyHighlighter.PY_FSTRING_FRAGMENT_BRACES))
        val COMMA = arrayOf(createTextAttributesKey("SMKSL_COMMA", PyHighlighter.PY_FSTRING_FRAGMENT_COLON))
        val STRING_CONTENT = arrayOf(createTextAttributesKey("SMKSL_STRING_CONTENT", PyHighlighter.PY_BYTE_STRING))
        val FORMAT_SPECIFIER =  arrayOf(createTextAttributesKey("SMKSL_FORMAT_SPECIFIER", PyHighlighter.PY_NUMBER))
        val ACCESS_KEY =  arrayOf(createTextAttributesKey("SMKSL_ACCESS_KEY", PyHighlighter.PY_KEYWORD_ARGUMENT))
    }

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        when {
            tokenType === SmkSLTokenTypes.LBRACE ||
                    tokenType === SmkSLTokenTypes.RBRACE -> BRACES
            tokenType === SmkSLTokenTypes.COMMA -> COMMA
            tokenType === SmkSLTokenTypes.STRING_CONTENT -> STRING_CONTENT
            tokenType === SmkSLTokenTypes.FORMAT_SPECIFIER -> FORMAT_SPECIFIER
            tokenType === SmkSLTokenTypes.ACCESS_KEY -> ACCESS_KEY
            else -> emptyArray()
        }

    override fun getHighlightingLexer() = SmkSLLexerAdapter()
}
