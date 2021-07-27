package com.jetbrains.snakecharm.lang.highlighter

import com.jetbrains.python.lexer.PythonHighlightingLexer
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.WORKFLOW_TOPLEVEL_DECORATOR_KEYWORD
import com.jetbrains.snakecharm.lang.parser.SnakemakeLexer

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeHighlightingLexer(level: LanguageLevel) : PythonHighlightingLexer(level) {
    override fun getTokenType() =
        if (tokenText in SnakemakeLexer.TOPLEVEL_KEYWORDS) {
            WORKFLOW_TOPLEVEL_DECORATOR_KEYWORD
        } else {
            SnakemakeLexer.KEYWORDS[tokenText] ?: super.getTokenType()
        }
}