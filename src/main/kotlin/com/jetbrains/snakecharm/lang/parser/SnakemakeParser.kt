package com.jetbrains.snakecharm.lang.parser

import com.intellij.lang.PsiBuilder
import com.jetbrains.python.parsing.ParsingContext
import com.jetbrains.python.parsing.PyParser
import com.jetbrains.python.parsing.StatementParsing
import com.jetbrains.python.psi.LanguageLevel

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeParser : PyParser() {
    override fun createParsingContext(
            builder: PsiBuilder,
            languageLevel: LanguageLevel,
            futureFlag: StatementParsing.FUTURE?
    ): ParsingContext = SmkParserContext(builder, languageLevel, futureFlag)
}