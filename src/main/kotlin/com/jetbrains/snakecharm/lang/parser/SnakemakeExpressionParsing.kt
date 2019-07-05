package com.jetbrains.snakecharm.lang.parser

import com.intellij.psi.tree.IElementType
import com.jetbrains.python.PyBundle.message
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.parsing.ExpressionParsing
import com.jetbrains.python.parsing.Parsing
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakecharm.SnakemakeBundle

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-04
 */
class SnakemakeExpressionParsing(context: SnakemakeParserContext) : ExpressionParsing(context) {
    override fun getParsingContext() = myContext as SnakemakeParserContext

    fun parseRuleParamArgumentList() =
            parseArgumentList(
                    PyTokenTypes.COMMA,
                    message("PARSE.expected.expression")
            ) { parseRuleParamArgument() }

    fun parseArgumentList(
            separatorToken: PyElementType,
            errorMessage: String,
            parsingFunction: () -> Boolean
    ): Boolean {
        val context = myContext
        val scope = context.scope as SnakemakeParsingScope
        myContext.pushScope(scope.withParamsArgsList())
        val result = doParseRuleParamArgumentList(separatorToken) {
            parseArgumentAndReportErrors(errorMessage, separatorToken, parsingFunction)
        }
        context.popScope()
        return result
    }

    private fun doParseRuleParamArgumentList(
            separatorToken: PyElementType,
            parseArgumentFunction: () -> Unit
    ): Boolean {
        // let's make ':' part of arg list, similar as '(', ')' are parts of arg list
        // helps with formatting, e.g. enter handler
        val hasColon = myBuilder.tokenType == PyTokenTypes.COLON
        if (!hasColon) {
            myBuilder.error(message("PARSE.expected.colon"))
        }
        val argList = myBuilder.mark()
        if (hasColon) {
            myBuilder.advanceLexer()
        }

        val argsOnNextLine = myBuilder.tokenType === PyTokenTypes.STATEMENT_BREAK
        if (argsOnNextLine) {
            nextToken()
            if (!checkMatches(PyTokenTypes.INDENT, "Indent expected...")) { // bundle
                argList.done(PyElementTypes.ARGUMENT_LIST)
                return false
            }
        }
        var indents = if (argsOnNextLine) 1 else 0
        var argNumber = 0
        while (!myBuilder.eof() && !atToken(PyTokenTypes.STATEMENT_BREAK)) {
            argNumber++

            // separator if several args:
            if (argNumber > 1) {
                if (matchToken(separatorToken)) {
                    val separatorMarker = myBuilder.mark()
                    val separatorMarkerIndents = indents

                    // skip indents/dedents:
                    if (matchToken(PyTokenTypes.STATEMENT_BREAK)) {
                        // skip indents
                        if (matchToken(PyTokenTypes.INDENT)) {
                            indents++
                        } else {
                            // skip dedents while matched, we could have several dedent tokens in a raw
                            // skip dedent while inside current block (indents > 1)
                            while (!myBuilder.eof() && atToken(PyTokenTypes.DEDENT) && indents > 1) {
                                indents--
                                nextToken()
                            }
                        }
                    }

                    // Case: hanging separator, next statement is another rule param block
                    if (myBuilder.tokenType === PyTokenTypes.DEDENT ||
                            (myBuilder.tokenType == PyTokenTypes.IDENTIFIER &&
                                    myBuilder.lookAhead(1) == PyTokenTypes.COLON)) {
                        indents = separatorMarkerIndents
                        separatorMarker.rollbackTo()
                        break
                    } else {
                        separatorMarker.drop()
                    }

                    if (myBuilder.eof()) {
                        break
                    }
                } else {
                    separatorToken.specialMethodName
                    recoverUntilMatches(
                            SnakemakeBundle.message("PARSE.expected.separator.message", separatorToken.toString()),
                            separatorToken,
                            PyTokenTypes.STATEMENT_BREAK
                    )
                }
            }

            parseArgumentFunction()
        }
        nextToken()

        // Eat all matching dedents
        while (indents > 0 && !myBuilder.eof()) {
            if (checkMatches(PyTokenTypes.DEDENT, "Dedent expected")) { // bundle
                indents--
            } else {
                break
            }
        }
        argList.done(PyElementTypes.ARGUMENT_LIST)

        return true
    }

    private fun parseArgumentAndReportErrors(
            errorMessage: String,
            separatorToken: PyElementType,
            parseArgumentFunction: () -> Boolean
    ) {
        if (!parseArgumentFunction()) {
            recoverUntilMatches(errorMessage, separatorToken, PyTokenTypes.STATEMENT_BREAK)
        }
    }

    private fun parseRuleParamArgument(): Boolean {
        // *args or **kw
        if (myBuilder.tokenType === PyTokenTypes.MULT || myBuilder.tokenType === PyTokenTypes.EXP) {
            val starArgMarker = myBuilder.mark()
            myBuilder.advanceLexer()
            if (!parseSingleExpression(false)) {
                myBuilder.error(message("PARSE.expected.expression"))
            }
            starArgMarker.done(PyElementTypes.STAR_ARGUMENT_EXPRESSION)
        } else {
            // arg or named arg:
            if (Parsing.isIdentifier(myBuilder)) {
                val keywordArgMarker = myBuilder.mark()
                Parsing.advanceIdentifierLike(myBuilder)
                if (myBuilder.tokenType === PyTokenTypes.EQ) {
                    myBuilder.advanceLexer()
                    if (!parseSingleExpression(false)) {
                        myBuilder.error(message("PARSE.expected.expression"))
                    }
                    keywordArgMarker.done(PyElementTypes.KEYWORD_ARGUMENT_EXPRESSION)
                    return true
                }
                keywordArgMarker.rollbackTo()
            }
            if (!parseSingleExpression(false)) {
                myBuilder.error(message("PARSE.expected.expression"))
                return false
            }
        }
        return true
    }


    /**
     * Skips tokens until token from expected set and marks it with error
     */
    private fun recoverUntilMatches(errorMessage: String, vararg types: IElementType) {
        val errorMarker = myBuilder.mark()
        var hasNonWhitespaceTokens = false
        while (!(atAnyOfTokens(*types) || myBuilder.eof())) {
            // Regular whitespace tokens are already skipped by advancedLexer()
            if (!atToken(PyTokenTypes.STATEMENT_BREAK)) {
                hasNonWhitespaceTokens = true
            }
            myBuilder.advanceLexer()
        }
        if (hasNonWhitespaceTokens) {
            errorMarker.error(errorMessage)
        } else {
            errorMarker.drop()
        }
    }
}