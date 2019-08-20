package com.jetbrains.snakecharm.string_language

import com.intellij.lang.*
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.ILazyParseableElementType
import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakecharm.string_language.lang.psi.SmkSLReferenceExpression
import org.intellij.lang.regexp.RegExpCapability
import org.intellij.lang.regexp.RegExpLanguage
import org.intellij.lang.regexp.RegExpParserDefinition
import java.util.*

class SmkSLTokenType(debugName: String) : IElementType(debugName, SmkSL)

class SmkSLRegExpElementType(debugName: String) : ILazyParseableElementType(debugName, SmkSL) {
    companion object {
        // Our choice of capabilities is based on
        // pattern for matching regular expressions in Snakemake:
        // ([^{}]+ | \{\d+(,\d+)?\})*
        // See snakemake/io.py:486
        val CAPABILITIES: EnumSet<RegExpCapability> = EnumSet.of(
                RegExpCapability.OCTAL_NO_LEADING_ZERO,
                RegExpCapability.MIN_OCTAL_3_DIGITS
        )
    }
    override fun parseLight(chameleon: ASTNode): PsiBuilder {
        val project = chameleon.psi.project
        val regExpLanguage = RegExpLanguage.INSTANCE
        val definition =
                LanguageParserDefinitions.INSTANCE.forLanguage(regExpLanguage) as RegExpParserDefinition
        val lexer = definition.createLexer(project, CAPABILITIES)
        val parser = definition.createParser(project, CAPABILITIES)
        val builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, lexer, regExpLanguage, chameleon.chars)
        (parser as LightPsiParser).parseLight(this, builder)

        return builder
    }

    override fun doParseContents(chameleon: ASTNode, psi: PsiElement): ASTNode? =
        parseLight(chameleon).treeBuilt.firstChildNode
}


object SmkSLTokenTypes {
    val REGEXP = SmkSLRegExpElementType("REGEXP")

    // PyToken for identifier is required for PyReferenceExpression to work properly
    val IDENTIFIER: IElementType = PyTokenTypes.IDENTIFIER

    val DOT = SmkSLTokenType("DOT")

    val LBRACE = SmkSLTokenType("LBRACE")

    val RBRACE = SmkSLTokenType("RBRACE")

    val STRING_CONTENT = SmkSLTokenType("STRING_CONTENT")

    val LBRACKET = SmkSLTokenType("LBRACKET")

    val RBRACKET = SmkSLTokenType("RBRACKET")

    val COMMA = SmkSLTokenType("COMMA")

    val UNEXPECTED_TOKEN = SmkSLTokenType("UNEXPECTED_TOKEN")

    val LANGUAGE = SmkSLTokenType("LANGUAGE")

    val SUBSCRIPTION_EXPRESSION = SmkSLTokenType("SUBSCRIPTION_EXPRESSION")

    val ACCESS_KEY = SmkSLTokenType("ACCESS_KEY")

    val REFERENCE_EXPRESSION = SmkSLTokenType("REFERENCE_EXPRESSION")

    val KEY_EXPRESSION = SmkSLTokenType("KEY_EXPRESSION")

    val EXPRESSION_STATEMENT = SmkSLTokenType("EXPRESSION_STATEMENT")
}
