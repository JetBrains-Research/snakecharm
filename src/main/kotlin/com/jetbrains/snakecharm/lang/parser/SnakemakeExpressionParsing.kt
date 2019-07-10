package com.jetbrains.snakecharm.lang.parser

import com.intellij.lang.impl.PsiBuilderImpl
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

    fun parseRuleLikeSectionArgumentList() = parseArgumentList(
            ",", PyTokenTypes.COMMA,
            message("PARSE.expected.expression"),
            this::parseRuleParamArgument
    )

    fun parseArgumentList(
            separatorTokenText: String,
            separatorTokenType: PyElementType,
            errorMessage: String,
            parsingFunction: () -> Boolean
    ): Boolean {
        val context = myContext
        val scope = context.scope as SnakemakeParsingScope
        myContext.pushScope(scope.withParamsArgsList())
        val result = doParseRuleParamArgumentList(separatorTokenText, separatorTokenType) {
            parseArgumentAndReportErrors(errorMessage, separatorTokenType, parsingFunction)
        }
        context.popScope()
        return result
    }

    private fun doParseRuleParamArgumentList(
            separatorTokenText: String,
            separatorTokenType: PyElementType,
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
        while (!myBuilder.eof()) {
            if (atToken(PyTokenTypes.STATEMENT_BREAK)) {
                println(myBuilder.rawLookup(2))
                nextToken()
                println(myBuilder.rawLookup(1))
                /*
                 * IMPORTANT: PLEASE READ BEFORE MODIFYING OR DEBUGGING!
                 * if you check token type here or anywhere else via any method that uses getTokenType(),
                 * e.g. atToken(), matchToken(), etc.,
                 * it might apply filter and change the token type,
                 * while nextToken() does not do that,
                 * it just increases the current lexeme number, doesn't look at the token
                 */
                if (indents == 0 &&
                        !findTokenRawLookup(separatorTokenType) &&
                        !findTokenRawLookup(PyTokenTypes.INDENT) &&
                        !findTokenRawLookup(PyTokenTypes.INCONSISTENT_DEDENT) &&
                        !findTokenRawLookup(PyTokenTypes.DEDENT)) {
                    println(myBuilder.rawLookup(1))
                    break
                }

                /*
                 * if an indent/dedent/inconsistent dedent/separator token was found,
                 * it's ok to match it, no filters will be applied
                 */
                if (indents > 0 && atToken(separatorTokenType)) {
                    continue
                }

                if (matchToken(PyTokenTypes.INDENT)) {
                    indents++
                } else {
                    while (!myBuilder.eof() && indents > 0) {
                        if (atToken(PyTokenTypes.INCONSISTENT_DEDENT)) {
                            myBuilder.error("Unindent does not match any outer indentation level")
                            nextToken()
                        }
                        // IMPORTANT: keep this check inside the loop body
                        /* atToken() uses getTokenType() which might apply a filter to the current token
                           whether or not this token is actually DEDENT */
                        else if (atToken(PyTokenTypes.DEDENT)) {
                            nextToken()
                            indents--
                        } else {
                            break
                        }
                    }
                    // leave this section
                    if (indents == 0 || myBuilder.eof()) {
                        break
                    }
                }
            }

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
                            while (!myBuilder.eof() && atToken(PyTokenTypes.DEDENT) && indents > 1) {
                                indents--
                                nextToken()
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
                    recoverUntilMatches(
                            SnakemakeBundle.message("PARSE.expected.separator.message", separatorTokenText),
                            separatorTokenType,
                            PyTokenTypes.STATEMENT_BREAK
                    )
                }
            }

            parseArgumentFunction()
        }

        if (myBuilder.rawLookup(0) == PyTokenTypes.STATEMENT_BREAK) {
            nextToken()
            while (indents > 0 && !myBuilder.eof()) {
                if (checkMatches(PyTokenTypes.DEDENT, "Dedent expected")) { // bundle
                    indents--
                } else {
                    break
                }
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

    private fun findTokenRawLookup(tokenType: IElementType): Boolean {
        var steps = 0
        // TODO can it somehow be not PsiBuilderImpl?
        while (myBuilder.rawLookup(steps) != null &&
                (myBuilder as PsiBuilderImpl).whitespaceOrComment(myBuilder.rawLookup(steps))) {
            steps++
        }
        return myBuilder.rawLookup(steps) == tokenType
    }
}