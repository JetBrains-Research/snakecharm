package com.jetbrains.snakecharm.lang.parser

import com.jetbrains.python.PyBundle
import com.jetbrains.python.PyBundle.message
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.parsing.ExpressionParsing
import com.jetbrains.python.parsing.Parsing
import com.jetbrains.python.psi.PyElementType

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-04
 */
class SnakemakeExpressionParsing(context: SnakemakeParserContext) : ExpressionParsing(context) {
    override fun getParsingContext() = myContext as SnakemakeParserContext

    fun parseRuleParamArgumentList(): Boolean {
        val context = myContext
        val scope = context.scope as SnakemakeParsingScope
        myContext.pushScope(scope.withParamsArgsList())
        val result = doParseRuleParamArgumentList()
        context.popScope()
        return result
    }

    private fun doParseRuleParamArgumentList(): Boolean {
        // let's make ':' part of arg list, similar as '(', ')' are parts of arg list
        // helps with formatting, e.g. enter handler
        val hasColon = myBuilder.tokenType == PyTokenTypes.COLON
        if (!hasColon) {
            myBuilder.error(PyBundle.message("PARSE.expected.colon"))
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

            // comma if several args:
            if (argNumber > 1) {
                if (matchToken(PyTokenTypes.COMMA)) {
                    val commaMarker = myBuilder.mark()
                    val commMarkerIndents = indents

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

                    // Case: hanging 'comma', next statement is another rule param block
                    if (myBuilder.tokenType === PyTokenTypes.DEDENT ||
                            (myBuilder.tokenType == PyTokenTypes.IDENTIFIER &&
                                    myBuilder.lookAhead(1) == PyTokenTypes.COLON)) {
                        indents = commMarkerIndents
                        commaMarker.rollbackTo()
                        break
                    } else {
                        commaMarker.drop()
                    }

                    if (myBuilder.eof()) {
                        break
                    }
                } else {
                    myBuilder.error(message("PARSE.expected.comma.or.rpar"))
                    break
                }

            }

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
                        continue
                    }
                    keywordArgMarker.rollbackTo()
                }
                if (!parseSingleExpression(false)) {
                    myBuilder.error(message("PARSE.expected.expression"))
                    break
                }
            }
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

    fun parseRulesList(
            separatorToken: PyElementType,
            separatorMissingMsg: String,
            ruleMissingMsg: String
    ): Boolean {
        matchToken(PyTokenTypes.STATEMENT_BREAK)
        matchToken(PyTokenTypes.INDENT)

        val argList = myBuilder.mark()

        var result = true
        var argsNumber = 0
        while (!myBuilder.eof()) {
            if (matchToken(PyTokenTypes.STATEMENT_BREAK)) {
                if (matchToken(PyTokenTypes.DEDENT)) {
                    while (matchToken(PyTokenTypes.DEDENT)) {}
                    break
                }
                if (!matchToken(PyTokenTypes.INDENT)) {
                    break
                }
            }

            argsNumber++
            if (argsNumber > 1) {
                if (!checkMatches(separatorToken, separatorMissingMsg)) { // bundle
                    result = false
                    nextToken()
                } else {
                    matchToken(PyTokenTypes.STATEMENT_BREAK)
                    matchToken(PyTokenTypes.INDENT)
                    matchToken(PyTokenTypes.INCONSISTENT_DEDENT)
                    matchToken(PyTokenTypes.DEDENT)
                }
            }

            if (matchToken(PyTokenTypes.STATEMENT_BREAK)) {
                if (matchToken(PyTokenTypes.DEDENT)) {
                    break
                }
                if (!matchToken(PyTokenTypes.INDENT)) {
                    break
                }
            }

            if (!checkMatches(PyTokenTypes.IDENTIFIER, ruleMissingMsg)) {
                result = false
                nextToken()
            }
        }

        argList.done(PyElementTypes.ARGUMENT_LIST)

        return result
    }
}