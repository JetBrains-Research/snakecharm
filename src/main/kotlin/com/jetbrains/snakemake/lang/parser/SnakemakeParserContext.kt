package com.jetbrains.snakemake.lang.parser

import com.intellij.lang.PsiBuilder
import com.jetbrains.python.parsing.ExpressionParsing
import com.jetbrains.python.parsing.FunctionParsing
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

    // See CythonParsingContext

    private val stmtParser = SnakemakeStatementParsing(this, futureFlag)

    override fun getScope(): SnakemakeParsingScope {
        val scope = super.getScope()
        require(scope is SnakemakeParsingScope)
        return scope
    }

    override fun getStatementParser() = stmtParser

    override fun getExpressionParser(): ExpressionParsing {
        return super.getExpressionParser()
    }

    override fun getFunctionParser(): FunctionParsing {
        return super.getFunctionParser()
    }

    override fun emptyParsingScope() = SnakemakeParsingScope()
}