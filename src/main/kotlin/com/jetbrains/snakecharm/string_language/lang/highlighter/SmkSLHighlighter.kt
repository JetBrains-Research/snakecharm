package com.jetbrains.snakecharm.string_language.lang.highlighter

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.jetbrains.python.highlighting.PyHighlighter
import com.jetbrains.snakecharm.string_language.SmkSLTokenTypes
import com.jetbrains.snakecharm.string_language.lang.parser.SmkSLLexerAdapter
import java.awt.font.TextAttribute

class SmkSLHighlighter : SyntaxHighlighterBase() {
    companion object {
        val BRACES = arrayOf(createTextAttributesKey("BRACES", PyHighlighter.PY_FSTRING_FRAGMENT_BRACES))
        val COMMA = arrayOf(createTextAttributesKey("COMMA", PyHighlighter.PY_FSTRING_FRAGMENT_COLON))
        val STRING_CONTENT = arrayOf(createTextAttributesKey("STRING_CONTENT", PyHighlighter.PY_BYTE_STRING))
        val ACCESS_KEY =  arrayOf(createTextAttributesKey("ACCESS_KEY", PyHighlighter.PY_NUMBER))
    }

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
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