package com.jetbrains.snakecharm.lang.highlighter

import com.jetbrains.python.lexer.PythonHighlightingLexer
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.snakecharm.lang.parser.SnakemakeLexer

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeHighlightingLexer(level: LanguageLevel): PythonHighlightingLexer(level) {
    override fun getTokenType() = SnakemakeLexer.KEYWORDS[tokenText] ?: super.getTokenType()
}