package com.jetbrains.snakecharm.stringLanguage.lang.parser

import com.intellij.psi.tree.IElementType
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.snakecharm.stringLanguage.lang.psi.SmkSLElementType
import com.jetbrains.snakecharm.stringLanguage.lang.psi.elementTypes.SmkSLRegExpElementType

object SmkSLTokenTypes {
    val REGEXP = SmkSLRegExpElementType("REGEXP")

    // PyToken for identifier is required for PyReferenceExpression to work properly
    val IDENTIFIER: IElementType = PyTokenTypes.IDENTIFIER

    val ACCESS_KEY = SmkSLElementType("ACCESS_KEY")

    val DOT = SmkSLElementType("DOT")

    val LBRACE = SmkSLElementType("LBRACE")

    val RBRACE = SmkSLElementType("RBRACE")

    val STRING_CONTENT = SmkSLElementType("STRING_CONTENT")

    val LBRACKET = SmkSLElementType("LBRACKET")

    val RBRACKET = SmkSLElementType("RBRACKET")

    val COMMA = SmkSLElementType("COMMA")

    val BAD_CHARACTER = PyTokenTypes.BAD_CHARACTER!!
    // val SPACE = PyTokenTypes.SPACE!!
    // val TAB = PyTokenTypes.TAB!!

    val FORMAT_SPECIFIER = SmkSLElementType("FORMAT_SPECIFIER")

    val FORMAT_SPECIFIER_EXPRESSION = SmkSLElementType("FORMAT_SPECIFIER_EXPRESSION")
}
