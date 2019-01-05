package com.jetbrains.snakemake.lang.parser

import com.intellij.lang.PsiBuilder
import com.jetbrains.python.PyBundle
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.parsing.StatementParsing
import com.jetbrains.snakemake.lang.psi.SMKRuleParameterListStatement
import com.jetbrains.snakemake.lang.psi.SMKRuleRunParameter
import com.jetbrains.snakemake.lang.psi.elementTypes.SnakemakeElementTypes

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeStatementParsing(
        context: SnakemakeParserContext,
        futureFlag: FUTURE?
) : StatementParsing(context, futureFlag) {

    override fun getParsingContext() = myContext as SnakemakeParserContext

    // TODO cleanup
//    override fun getReferenceType(): IElementType {
//            return CythonElementTypes.REFERENCE_EXPRESSION
//        }

    override fun parseStatement() {
        // TODO cleanup:
//        val context = parsingContext
//        val scope = context.scope as SnakemakeParsingScope
//        var isRule = scope.isRule
        // myBuilder.setDebugMode(true)

        if (atToken(SnakemakeTokenTypes.RULE_KEYWORD)) {
//            isRule = true
            val ruleMarker: PsiBuilder.Marker = myBuilder.mark()
            nextToken()

            // rule name
            if (myBuilder.tokenType == PyTokenTypes.IDENTIFIER) {
                nextToken()
            }
            checkMatches(PyTokenTypes.COLON, "Identifier or ':' expected") // bundle
            checkEndOfStatement()
            checkMatches(PyTokenTypes.INDENT, "Indent expected...") // bundle
            while (!myBuilder.eof() && myBuilder.tokenType !== PyTokenTypes.DEDENT) {
                if (!parseRuleParameter(myBuilder)) {
                    break
                }
            }
            ruleMarker.done(SnakemakeElementTypes.RULE_DECLARATION)
             nextToken()

        } else {
            super.parseStatement()
        }

    }

    private fun parseRuleParameter(builder: PsiBuilder): Boolean {
        val keyword = builder.tokenText
        val ruleParam = builder.mark()

        if (!checkMatches(PyTokenTypes.IDENTIFIER, IDENTIFIER_EXPECTED)) {
            ruleParam.drop()
            return false
        }

        checkMatches(PyTokenTypes.COLON, PyBundle.message("PARSE.expected.colon"))

        var result = false

        when (keyword) {
            in SMKRuleParameterListStatement.KEYWORDS -> {
                // TODO: probably do this behaviour by default and use inspection error
                // instead of parsing errors..
                result = parsingContext.expressionParser.parseRuleParamArgumentList()
                ruleParam.done(SnakemakeElementTypes.RULE_PARAMETER_LIST_STATEMENT)
            }
            in SMKRuleRunParameter.KEYWORDS -> {
                statementParser.parseSuite()
                ruleParam.done(SnakemakeElementTypes.RULE_RUN_STATEMENT)
            }
            else -> {
                // error
                myBuilder.error("Unexpected keyword $keyword in rule definition") // bundle

                //TODO advance until eof or STATEMENT_END?
                // checkEndOfStatement()
                ruleParam.drop()
            }
        }
        return result
    }

    // TODO: cleanup
//    override fun getFunctionParser(): FunctionParsing {
//        return super.getFunctionParser()
//    }
}