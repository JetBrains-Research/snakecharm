package com.jetbrains.snakecharm.string_language

import com.intellij.psi.tree.IElementType

class SmkSLTokenType(debugName: String) : IElementType(debugName, SmkStringLanguage)

object SmkSLTokenTypes {
    val DOT = SmkSLTokenType("DOT")

    val LBRACE = SmkSLTokenType("LBRACE")

    val RBRACE = SmkSLTokenType("RBRACE")

    val IDENTIFIER = SmkSLTokenType("IDENTIFIER")

    val STRING_CONTENT = SmkSLTokenType("STRING_CONTENT")

    val LBRACKET = SmkSLTokenType("LBRACKET")

    val RBRACKET = SmkSLTokenType("RBRACKET")

    val COMMA = SmkSLTokenType("COMMA")

    val REGEXP = SmkSLTokenType("REGEXP")

    val ACCESS_KEY = SmkSLTokenType("ACCESS_KEY")

    val UNEXPECTED_TOKEN = SmkSLTokenType("UNEXPECTED_TOKEN")

    val LANGUAGE = SmkSLTokenType("LANGUAGE")

    val SUBSCRIPTION_EXPRESSION = SmkSLTokenType("SUBSCRIPTION_EXPRESSION")

    val REFERENCE_EXPRESSION = SmkSLTokenType("REFERENCE_EXPRESSION")

    val EXPRESSION_STATEMENT = SmkSLTokenType("EXPRESSION_STATEMENT")
}