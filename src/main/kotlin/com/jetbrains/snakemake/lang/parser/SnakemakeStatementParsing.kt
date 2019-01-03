package com.jetbrains.snakemake.lang.parser

import com.intellij.lang.PsiBuilder
import com.jetbrains.python.PyBundle
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.parsing.FunctionParsing
import com.jetbrains.python.parsing.StatementParsing
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

    // TODO
//    override fun getReferenceType(): IElementType {
//            return CythonElementTypes.REFERENCE_EXPRESSION
//        }

    override fun parseStatement() {
        // super.parseStatement()

        // TODO:
//        val context = parsingContext
//        val scope = context.scope as SnakemakeParsingScope
//        var isRule = scope.isRule
        val builder = myContext.builder
        var marker: PsiBuilder.Marker? = null
//
        if (atToken(SnakemakeTokenTypes.RULE_KEYWORD)) run {
//            isRule = true
            marker = builder.mark()
            nextToken()

            // rule name
            if (builder.tokenType == PyTokenTypes.IDENTIFIER) {
                nextToken()
            }
            checkMatches(PyTokenTypes.COLON, "Identifier or ':' expected") // bundle
            checkEndOfStatement()
            checkMatches(PyTokenTypes.INDENT, "Indent expected...") // bundle
            while (!myBuilder.eof() && myBuilder.tokenType !== PyTokenTypes.DEDENT) {
                if (!parseRuleParameter(builder)) {
                    break
                }
            }
            marker!!.done(SnakemakeElementTypes.RULE)
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
        if (keyword == "input" || keyword == "output") {
            //TODO: parse arg list
            //no: expressionParser.parseArgumentList()
            val argList = builder.mark()
            nextToken()
            argList.done(PyElementTypes.ARGUMENT_LIST)

            ruleParam.done(SnakemakeElementTypes.RULE_PARAMETER_LIST)
            result = true
        } else {
            // error
            myBuilder.error("Unexpected keyword $keyword in rule definition") // bundle

            ruleParam.drop()
        }
        checkEndOfStatement()
        return result
    }

    override fun getFunctionParser(): FunctionParsing {
        return super.getFunctionParser()
    }
}