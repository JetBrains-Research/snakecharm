package com.jetbrains.snakecharm.lang.highlighter

import com.intellij.psi.tree.IElementType
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.lexer.PythonHighlightingLexer
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes
import com.jetbrains.snakecharm.lang.parser.SnakemakeLexer

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeHighlightingLexer(level: LanguageLevel) : PythonHighlightingLexer(level) {
    override fun getTokenType(): IElementType? {
        val tokenType = super.getTokenType()
        val remappedTokenType = when (tokenType) {
            PyTokenTypes.IDENTIFIER -> SnakemakeLexer.KEYWORD_LIKE_SECTION_NAME_2_TOKEN_TYPE[tokenText]  //TODO: maybe add known rule section names?
            PyTokenTypes.AS_KEYWORD -> SmkTokenTypes.SMK_AS_KEYWORD
            PyTokenTypes.FROM_KEYWORD -> SmkTokenTypes.SMK_FROM_KEYWORD
            PyTokenTypes.WITH_KEYWORD -> SmkTokenTypes.SMK_WITH_KEYWORD
            else -> null
        }
        return remappedTokenType ?: tokenType
    }
}
