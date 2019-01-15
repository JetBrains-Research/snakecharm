package com.jetbrains.snakecharm.lang.parser

import com.intellij.lang.PsiBuilder
import com.jetbrains.python.parsing.ParsingContext
import com.jetbrains.python.parsing.StatementParsing
import com.jetbrains.python.psi.LanguageLevel

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeParserContext(
        builder: PsiBuilder,
        languageLevel: LanguageLevel,
        futureFlag: StatementParsing.FUTURE?
): ParsingContext(builder, languageLevel, futureFlag) {

    private val stmtParser = SnakemakeStatementParsing(this, futureFlag)
    private val exprParser = SnakemakeExpressionParsing(this)

    override fun getScope(): SnakemakeParsingScope {
        val scope = super.getScope()
        require(scope is SnakemakeParsingScope)
        return scope
    }

    override fun getStatementParser() = stmtParser

    override fun getExpressionParser() = exprParser

//    override fun getFunctionParser(): FunctionParsing {
//        // TODO: cleanup
//        return super.getFunctionParser()
//    }

    override fun emptyParsingScope() = SnakemakeParsingScope()
}