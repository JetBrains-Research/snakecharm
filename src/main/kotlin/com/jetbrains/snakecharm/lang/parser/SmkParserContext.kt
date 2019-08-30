package com.jetbrains.snakecharm.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.impl.PsiBuilderImpl
import com.jetbrains.python.parsing.ParsingContext
import com.jetbrains.python.parsing.StatementParsing
import com.jetbrains.python.psi.LanguageLevel

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SmkParserContext(
        builder: PsiBuilder,
        languageLevel: LanguageLevel,
        futureFlag: StatementParsing.FUTURE?
): ParsingContext(builder, languageLevel, futureFlag) {

    init {
        require(builder is PsiBuilderImpl) {
            "Parser must be PsiBuilderImpl in order for Snakemake parser to work properly"
        }

    }

    private val stmtParser = SmkStatementParsing(this, futureFlag)
    private val exprParser = SmkExpressionParsing(this)
    private val funParser = SmkFunctionParsing(this)

    override fun getScope(): SmkParsingScope {
        val scope = super.getScope()
        require(scope is SmkParsingScope)
        return scope
    }

    override fun getStatementParser() = stmtParser

    override fun getExpressionParser() = exprParser

    override fun getFunctionParser() = funParser

    override fun emptyParsingScope() = SmkParsingScope()
}