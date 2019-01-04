package com.jetbrains.snakemake.lang.parser

import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.parsing.ExpressionParsing

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-04
 */
class SnakemakeExpressionParsing(context: SnakemakeParserContext): ExpressionParsing(context) {
    override fun getParsingContext() = myContext as SnakemakeParserContext

    fun parseRuleParamArgumentList(): Boolean {
        val argList = myBuilder.mark()

        //var genexpr: PsiBuilder.Marker? = myBuilder.mark()
        var argNumber = 0

        val stopToken = if (myBuilder.tokenType === PyTokenTypes.STATEMENT_BREAK) {
            nextToken()
            if (!checkMatches(PyTokenTypes.INDENT, "Indent expected...")) { // bundle
                argList.done(PyElementTypes.ARGUMENT_LIST)
                return false
            }
            PyTokenTypes.DEDENT
        } else {
            PyTokenTypes.STATEMENT_BREAK
        }
        while (!myBuilder.eof() && myBuilder.tokenType !== stopToken) {
            // TODO
            argNumber++
            nextToken()
        }
        nextToken()
        argList.done(PyElementTypes.ARGUMENT_LIST)

        return true
    }

}