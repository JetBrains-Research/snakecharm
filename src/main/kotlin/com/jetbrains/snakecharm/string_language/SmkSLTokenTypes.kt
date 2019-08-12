package com.jetbrains.snakecharm.string_language

import com.intellij.lang.*
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.ILazyParseableElementType
import org.intellij.lang.regexp.RegExpLanguage
import org.intellij.lang.regexp.RegExpParserDefinition

class SmkSLTokenType(debugName: String) : IElementType(debugName, SmkSL)

class RegExpElementType(debugName: String) : ILazyParseableElementType(debugName, SmkSL) {
    override fun parseLight(chameleon: ASTNode): PsiBuilder {
        val project = chameleon.psi.project
        val regExpLanguage = RegExpLanguage.INSTANCE
        val definition =
                LanguageParserDefinitions.INSTANCE.forLanguage(regExpLanguage) as RegExpParserDefinition
        val capabilities =
                definition.defaultCapabilities
        val lexer = definition.createLexer(project, capabilities)
        val parser = definition.createParser(project, capabilities)
        val builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, lexer, regExpLanguage, chameleon.chars)
        (parser as LightPsiParser).parseLight(this, builder)

        return builder
    }

    override fun doParseContents(chameleon: ASTNode, psi: PsiElement): ASTNode? =
        parseLight(chameleon).treeBuilt.firstChildNode
}


object SmkSLTokenTypes {
    val REGEXP = RegExpElementType("REGEXP")

    val DOT = SmkSLTokenType("DOT")

    val LBRACE = SmkSLTokenType("LBRACE")

    val RBRACE = SmkSLTokenType("RBRACE")

    val IDENTIFIER = SmkSLTokenType("IDENTIFIER")

    val STRING_CONTENT = SmkSLTokenType("STRING_CONTENT")

    val LBRACKET = SmkSLTokenType("LBRACKET")

    val RBRACKET = SmkSLTokenType("RBRACKET")

    val COMMA = SmkSLTokenType("COMMA")

    val ACCESS_KEY = SmkSLTokenType("ACCESS_KEY")

    val UNEXPECTED_TOKEN = SmkSLTokenType("UNEXPECTED_TOKEN")

    val LANGUAGE = SmkSLTokenType("LANGUAGE")

    val SUBSCRIPTION_EXPRESSION = SmkSLTokenType("SUBSCRIPTION_EXPRESSION")

    val REFERENCE_EXPRESSION = SmkSLTokenType("REFERENCE_EXPRESSION")

    val EXPRESSION_STATEMENT = SmkSLTokenType("EXPRESSION_STATEMENT")
}