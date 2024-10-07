package com.jetbrains.snakecharm.lang.parser

import com.intellij.lang.SyntaxTreeBuilder
import com.intellij.psi.tree.IElementType
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyParsingBundle.message
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.parsing.ExpressionParsing
import com.jetbrains.python.parsing.Parsing
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes.SMK_PY_REFERENCE_EXPRESSION

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-04
 */
class SmkExpressionParsing(context: SmkParserContext) : ExpressionParsing(context) {
    override fun getParsingContext() = myContext as SmkParserContext

    fun parseRuleLikeSectionArgumentList() = parseArgumentList(
            ",", PyTokenTypes.COMMA,
            message("PARSE.expected.expression"),
            this::parseRuleParamArgument
    )

    override fun getReferenceType() = SMK_PY_REFERENCE_EXPRESSION

    fun parseArgumentList(
            separatorTokenText: String,
            separatorTokenType: PyElementType,
            errorMessage: String,
            parsingFunction: () -> Boolean
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
            if (!checkMatches(PyTokenTypes.INDENT, SnakemakeBundle.message("PARSE.expected.indent"))) {
                argList.done(PyElementTypes.ARGUMENT_LIST)
                return false
            }
        }
        var indents = if (argsOnNextLine) 1 else 0
        var argNumber = 0
        var incorrectUnindentMarker: SyntaxTreeBuilder.Marker? = null
        while (!myBuilder.eof() && !atToken(PyTokenTypes.STATEMENT_BREAK)) {
            argNumber++

            // separator if several args:
            if (argNumber > 1) {
                if (matchToken(separatorTokenType)) {
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

                            while (indents > 1 && !myBuilder.eof()) {
                                if (atToken(PyTokenTypes.INCONSISTENT_DEDENT)) {
                                    incorrectUnindentMarker = myBuilder.mark()
                                    nextToken()
                                }
                                if (atToken(PyTokenTypes.DEDENT)) {
                                    indents--
                                    nextToken()
                                } else {
                                    break
                                }
                            }
                        }

                        // Case: hanging 'comma', next statement is another rule param block
                        // statement break after comma, if 'indents == 0' => we just left arg list
                        if (indents == 0) {
                            // game over, let's go to next section

                            // rollback because after loop we expected to be at statement break
                            indents = separatorMarkerIndents
                            separatorMarker.rollbackTo()
                            break
                        }
                    }

                    if (myBuilder.tokenType === PyTokenTypes.DEDENT) {
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
                    @Suppress("KotlinConstantConditions")
                    if (atToken(PyTokenTypes.INCONSISTENT_DEDENT) && incorrectUnindentMarker == null) {
                        incorrectUnindentMarker = myBuilder.mark()
                    } else {
                        recoverUntilMatches(
                                SnakemakeBundle.message("PARSE.expected.separator.message", separatorTokenText),
                                separatorTokenType,
                                PyTokenTypes.STATEMENT_BREAK
                        )
                    }
                }
            }

            if (atToken(PyTokenTypes.INCONSISTENT_DEDENT) && incorrectUnindentMarker == null) {
                incorrectUnindentMarker = myBuilder.mark()
            }

            if (!parsingFunction()) {
                recoverUntilMatches(errorMessage, separatorTokenType, PyTokenTypes.STATEMENT_BREAK)
            }

            // mark everything that was incorrectly unindented
            incorrectUnindentMarker?.error(SnakemakeBundle.message("PARSE.incorrect.unindent"))
            incorrectUnindentMarker = null
        }

        // Comments are skipped after STATEMENT_BREAK, but we'd like to
        // Have 'next' comment out of current section
        var beforeArgListDone: SyntaxTreeBuilder.Marker = myBuilder.mark()
        if (atToken(PyTokenTypes.STATEMENT_BREAK)) {
            nextToken()
            while (indents > 0 && !myBuilder.eof()) {
                beforeArgListDone.drop()
                beforeArgListDone = myBuilder.mark()
                if (checkMatches(PyTokenTypes.DEDENT, SnakemakeBundle.message("PARSE.expected.dedent"))) {
                    indents--
                } else {
                    break
                }
            }
        }
        beforeArgListDone.rollbackTo()
        argList.done(PyElementTypes.ARGUMENT_LIST)
        return true
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
            if (isIdentifier(myBuilder)) {
                val keywordArgMarker = myBuilder.mark()
                advanceIdentifierLike(myBuilder)
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
            // Regular whitespace tokens are already skipped by atToken()
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