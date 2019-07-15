package com.jetbrains.snakecharm.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.jetbrains.python.PyBundle.message
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.parsing.ExpressionParsing
import com.jetbrains.python.parsing.Parsing
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakecharm.SnakemakeBundle
import java.lang.reflect.InvocationTargetException

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-04
 */
class SnakemakeExpressionParsing(context: SnakemakeParserContext) : ExpressionParsing(context) {
    private val stringLiteralTokenSet = TokenSet.create(PyTokenTypes.FSTRING_START, *PyTokenTypes.STRING_NODES.types)

    private var indents = 0

    override fun getParsingContext() = myContext as SnakemakeParserContext

    fun parseRuleLikeSectionArgumentList() = parseArgumentList(
            ",", PyTokenTypes.COMMA,
            message("PARSE.expected.expression")
    ) { parseRuleParamArgument() }

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

    /**
     * XXX -------------------------------------------------------------------
     * XXX IMPORTANT! PLEASE READ BEFORE MODIFYING PARSER CODE OR DEBUGGING!
     * XXX -------------------------------------------------------------------
     *
     * Due to [SnakemakeStatementParsing] utilizing filter method when the parser has entered a rule section,
     * behaviour in debug and at runtime could differ.
     * Be careful when using any method which calls [PsiBuilder.getTokenType]
     * (such as [Parsing.atToken], [Parsing.matchToken], etc.),
     * as [PsiBuilder.getTokenType] skips whitespace tokens and applies filter,
     * substituting tokenTypes in myBuilder.myLexTypes.
     * Meanwhile [Parsing.nextToken] only advances lexer by one lexeme and does not look at the current token.
     * [PsiBuilder.getTokenType] might be called during debug and apply filter, while it would not be called at runtime,
     * thus the different behaviour.
     *
     * If you need to check the current token type, best to avoid debugging in IDE and to use debug print.
     * One can get the current token type without filter via a call to [PsiBuilder.rawLookup] with steps = 0.
     * However, it's best to keep [PsiBuilder.rawLookup] calls to a minimum.
     */
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
            if (!checkMatches(PyTokenTypes.INDENT, SnakemakeBundle.message("PARSE.expected.indent"))) {
                argList.done(PyElementTypes.ARGUMENT_LIST)
                return false
            }
        }
        indents = if (argsOnNextLine) 1 else 0
        var argNumber = 0
        var incorrectUnindentMarker: PsiBuilder.Marker? = null
        while (!myBuilder.eof()) {
            if (atToken(PyTokenTypes.STATEMENT_BREAK)) {
                nextToken()
                /* It's important to use rawLookup() inside atAnyOfTokensSage() here
                   to avoid accidentally applying a filter to the current token. */
                if (indents == 0 &&
                        !atAnyOfTokensSafe(
                                separatorTokenType,
                                PyTokenTypes.INDENT,
                                PyTokenTypes.INCONSISTENT_DEDENT,
                                PyTokenTypes.DEDENT
                        )
                ) {
                    break
                }

                /*
                 * If an indent/dedent/inconsistent dedent/separator token was found,
                 * it's ok to match it, no filters will be applied
                 */
                if (indents > 0 && atToken(separatorTokenType)) {
                    continue
                }

                if (matchToken(PyTokenTypes.INDENT)) {
                    indents++
                } else {
                    // IMPORTANT: note that myBuilder.eof() could apply filter to lexemes
                    // which is why it must be evaluated second
                    while (indents > 0 && !myBuilder.eof()) {
                        // IMPORTANT: keep this check inside the loop body
                        /* atToken() uses getTokenType() which might apply a filter to the current token
                           whether or not this token is actually DEDENT,
                           which is why it's best not to invoke this method unless necessary. */
                        if (atToken(PyTokenTypes.DEDENT)) {
                            nextToken()
                            indents--
                        } else if (atToken(PyTokenTypes.INCONSISTENT_DEDENT)) {
                            myBuilder.error(SnakemakeBundle.message("PARSE.incorrect.unindent"))
                            nextToken()
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
                    recoverUntilMatches(
                            SnakemakeBundle.message("PARSE.expected.separator.message", separatorTokenText),
                            separatorTokenType,
                            PyTokenTypes.STATEMENT_BREAK
                    )
                }
            }

            parseArgumentFunction()
            // mark everything that was incorrectly unindented
            incorrectUnindentMarker?.error(SnakemakeBundle.message("PARSE.incorrect.unindent"))
            incorrectUnindentMarker = null
        }

        // safe check is important here, be wary of that while modifying/debugging
        if (checkCurrentTokenSafe(PyTokenTypes.STATEMENT_BREAK)) {
            nextToken()
            while (indents > 0 && !myBuilder.eof()) {
                if (checkMatches(PyTokenTypes.DEDENT, SnakemakeBundle.message("PARSE.expected.dedent"))) {
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
            if (!parseSingleExpressionOrStringLiteral()) {
                myBuilder.error(message("PARSE.expected.expression"))
                return false
            }
        }
        return true
    }

    private fun parseSingleExpressionOrStringLiteral(): Boolean {
        if (!parseStringAdditiveExpression()) {
            return parseSingleExpression(false)
        }
        return true
    }

    private fun atStringNodeOrFormattedString() = atAnyOfTokensSafe(*stringLiteralTokenSet.types)

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

    /**
     * Skips whitespace tokens and checks the first non-whitespace token type against [tokenTypes]
     * without applying filters.
     */
    private fun atAnyOfTokensSafe(vararg tokenTypes: IElementType): Boolean {
        var steps = 0
        while (myBuilder.rawLookup(steps) != null &&
                (myBuilder as PsiBuilderImpl).whitespaceOrComment(myBuilder.rawLookup(steps))) {
            steps++
        }
        return tokenTypes.contains(myBuilder.rawLookup(steps))
    }

    /**
     * Compares the current token against [tokenType] without applying filters.
     */
    private fun checkCurrentTokenSafe(tokenType: IElementType) = myBuilder.rawLookup(0) == tokenType

    private fun parseMultilineStringTwoPasses(): Boolean {
        if (!atStringNodeOrFormattedString()) {
            return false
        }

        var stringLiteralMarker = myBuilder.mark()
        var statementEndPosition = -1
        var statementBreakMarker: PsiBuilder.Marker? = null // still necessary as we might need to rollback to this
        var incorrectUnindentMarker: PsiBuilder.Marker? = null
        var previousIndents = indents

        // if true, we need a second pass to replace statement breaks with line breaks
        // and do call expression parsing on this multiline string
        var dotOccurred = false

        while (atStringNodeOrFormattedString()) {
            if (atAnyOfTokensSafe(PyTokenTypes.FSTRING_START)) {
                val parseFunction = ExpressionParsing::class.java.getDeclaredMethod("parseFormattedStringNode")
                parseFunction.isAccessible = true
                try {
                    parseFunction.invoke(this)
                } catch (e: InvocationTargetException) {
                    // TODO log or something? because it does happen sometimes bc of ProcessCanceledException
                    parseSingleExpression(false)
                }
            } else {
                nextToken()
            }

            if (incorrectUnindentMarker != null) {
                incorrectUnindentMarker.error(SnakemakeBundle.message("PARSE.incorrect.unindent"))
                incorrectUnindentMarker = null
            }

            // when lines are separated with '\' symbol or there are two strings in a row like this: "1" "2"
            if (atStringNodeOrFormattedString()) {
                continue
            }

            if (!atAnyOfTokensSafe(PyTokenTypes.STATEMENT_BREAK)) {
                if (atAnyOfTokensSafe(PyTokenTypes.PLUS)) {
                    statementEndPosition = myBuilder.rawTokenIndex()
                } else if (atAnyOfTokensSafe(PyTokenTypes.DOT)) {
                    statementEndPosition = myBuilder.rawTokenIndex()
                    dotOccurred = true
                }
                break
            } else {
                statementBreakMarker?.drop()
                statementBreakMarker = myBuilder.mark()
                statementEndPosition = myBuilder.rawTokenIndex()
                previousIndents = indents
                nextToken()
            }

            if (atAnyOfTokensSafe(PyTokenTypes.INDENT)) {
                nextToken()
                indents++
            } else {
                loop@ while (indents > 0 && !myBuilder.eof()) {
                    when {
                        atToken(PyTokenTypes.DEDENT) -> {
                            nextToken()
                            indents--
                        }
                        atToken(PyTokenTypes.INCONSISTENT_DEDENT) -> {
                            incorrectUnindentMarker = myBuilder.mark()
                            nextToken()
                        }
                        else -> break@loop
                    }
                }
                if (incorrectUnindentMarker == null && indents == 0) {
                    break
                }
            }
        }

        // second pass to replace all necessary statement breaks with line breaks
        // it has to be done on the 2nd pass when it is already known which position signifies the end of the expression
        if (dotOccurred) {
            myBuilder.setTokenTypeRemapper { source, _, _, _ ->
                if (source == PyTokenTypes.INDENT || source == PyTokenTypes.DEDENT) {
                    PyTokenTypes.SPACE
                } else {
                    source
                }
            }
            stringLiteralMarker.rollbackTo()
            if (statementEndPosition == -1) {
                return parseSingleExpression(false)
            }
            stringLiteralMarker = myBuilder.mark()
            while (myBuilder.rawTokenIndex() < statementEndPosition) {
                if (atStringNodeOrFormattedString()) {
                    nextToken()
                    incorrectUnindentMarker?.error(SnakemakeBundle.message("PARSE.incorrect.unindent"))
                    incorrectUnindentMarker = null
                    continue
                }
                println("${myBuilder.rawLookup(0)} ${myBuilder.rawLookup(1)} ${myBuilder.rawLookup(2)}")
                while (atAnyOfTokensSafe(PyTokenTypes.STATEMENT_BREAK, PyTokenTypes.INDENT, PyTokenTypes.DEDENT)) {
                    myBuilder.remapCurrentToken(PyTokenTypes.SPACE)
                    println("${myBuilder.rawLookup(0)} ${myBuilder.rawLookup(1)} ${myBuilder.rawLookup(2)}")
                    myBuilder.advanceLexer()
                    println("${myBuilder.rawLookup(0)} ${myBuilder.rawLookup(1)} ${myBuilder.rawLookup(2)}")
                }
                println()
                if (atToken(PyTokenTypes.INCONSISTENT_DEDENT)) {
                    incorrectUnindentMarker = myBuilder.mark()
                }
                nextToken()
            }
            stringLiteralMarker.rollbackTo()
            myBuilder.setTokenTypeRemapper { source, _, _, _ -> source }
            // third pass, this time to parse everything including the dot
            return parseSingleExpression(false)

        } else if (!atAnyOfTokensSafe(PyTokenTypes.PLUS)) {
            statementBreakMarker?.rollbackTo()
        } else {
            statementBreakMarker?.drop()
        }

        if (atAnyOfTokensSafe(PyTokenTypes.STATEMENT_BREAK)) {
            indents = previousIndents
        }
        stringLiteralMarker.done(PyElementTypes.STRING_LITERAL_EXPRESSION)
        return true
    }

    private fun parseStringAdditiveExpression(): Boolean {
        var expr = myBuilder.mark()
        if (!parseMultilineStringTwoPasses()) {
            expr.drop()
            return false
        } else {
            while (atToken(PyTokenTypes.PLUS)) {
                myBuilder.advanceLexer()

                skipStatementBreaksUpToTokens(*stringLiteralTokenSet.types)

                if (!parseMultilineStringTwoPasses()) {
                    myBuilder.error(message("PARSE.expected.expression"))
                }

                expr.done(PyElementTypes.BINARY_EXPRESSION)
                expr = expr.precede()
                skipStatementBreaksUpToTokens(PyTokenTypes.PLUS)
            }

            expr.drop()
            return true
        }
    }

    private fun skipStatementBreaksUpToTokens(vararg tokenTypes: IElementType) {
        val marker = myBuilder.mark()
        val previousIndents = indents
        while (atAnyOfTokensSafe(PyTokenTypes.STATEMENT_BREAK)) {
            nextToken()
            if (atAnyOfTokensSafe(PyTokenTypes.INDENT)) {
                indents++
                nextToken()
            } else {
                skipDedents { myBuilder.error(SnakemakeBundle.message("PARSE.incorrect.unindent")) }
            }
            if (indents == 0) {
                break
            }
        }
        if (atAnyOfTokensSafe(*tokenTypes)) {
            marker.drop()
        } else {
            indents = previousIndents
            marker.rollbackTo()
        }
    }

    private fun skipDedents(incorrectUnindentHandler: () -> Unit) {
        loop@ while (indents > 0 && !myBuilder.eof()) {
            when {
                atAnyOfTokensSafe(PyTokenTypes.DEDENT) -> {
                    nextToken()
                    indents--
                }
                atAnyOfTokensSafe(PyTokenTypes.INCONSISTENT_DEDENT) -> {
                    incorrectUnindentHandler()
                    nextToken()
                }
                else -> break@loop
            }
        }
    }
}