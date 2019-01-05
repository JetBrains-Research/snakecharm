package com.jetbrains.snakemake.lang.parser

import com.jetbrains.python.PyBundle.message
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.parsing.ExpressionParsing
import com.jetbrains.python.parsing.Parsing

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-04
 */
class SnakemakeExpressionParsing(context: SnakemakeParserContext): ExpressionParsing(context) {
    override fun getParsingContext() = myContext as SnakemakeParserContext

    fun parseRuleParamArgumentList(): Boolean {
        val argList = myBuilder.mark()

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
        while (!myBuilder.eof() && myBuilder.tokenType !== PyTokenTypes.STATEMENT_BREAK) {
            argNumber++

            // comma if several args:
            if (argNumber > 1) {
                if (myBuilder.tokenType === PyTokenTypes.COMMA) {
                    nextToken()
                    val commaMarker = myBuilder.mark()

                    // skip indents, dedents while matched
                    var tt = myBuilder.tokenType
                    while (!myBuilder.eof()) {
                        if (tt == PyTokenTypes.INDENT) {
                            indents++
                        } else if (tt == PyTokenTypes.DEDENT) {
                            indents--
                            if (indents == 0) {
                                break
                            }
                        } else if (tt !== PyTokenTypes.STATEMENT_BREAK) {
                            break
                        }
                        nextToken()
                        tt = myBuilder.tokenType
                    }

                    tt = myBuilder.tokenType
                    if (tt == PyTokenTypes.IDENTIFIER) {
                        // check if next token is rule parameter identifier

                        val identifier = myBuilder.tokenText
                        if (identifier in setOf("output", "input")) {
                            // exclude this token from 'current' args list subtree
                            commaMarker.rollbackTo()
                            break
                        }
                    } else if (tt == PyTokenTypes.DEDENT && indents == 0) {
                        // hanging 'comma', next statement is another rule param block
                        // TODO!!!!!!!!!!!!!
                        indents++
                        commaMarker.rollbackTo()
                        break
                    }
                    commaMarker.drop()
                    
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
        while (!myBuilder.eof() && indents > 0) {
            if (checkMatches(PyTokenTypes.DEDENT, "Dedent expected")) { // bundle
                indents--
            } else {
                break
            }
        }
        argList.done(PyElementTypes.ARGUMENT_LIST)

        return true
    }

}