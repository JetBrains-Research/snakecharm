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

/**
 * @author Roman.Chernyatchik
 * @date 2019-01-04
 */
class SnakemakeExpressionParsing(context: SnakemakeParserContext) : ExpressionParsing(context) {
    private val stringLiteralTokenSet = TokenSet.create(PyTokenTypes.FSTRING_START, *PyTokenTypes.STRING_NODES.types)
    private val indentationTokenSet = TokenSet.create(
            PyTokenTypes.INDENT,
            PyTokenTypes.DEDENT,
            PyTokenTypes.INCONSISTENT_DEDENT
    )

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
                if (indents == 0 && !atAnyOfTokensSafe(separatorTokenType, *indentationTokenSet.types)) {
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
                    skipDedents { myBuilder.error(SnakemakeBundle.message("PARSE.incorrect.unindent")) }
                    // leave this rule section
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
                            skipDedents(1) {
                                if (incorrectUnindentMarker == null) {
                                    incorrectUnindentMarker = myBuilder.mark()
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
            if (!parseSingleExpressionOrStringExpression()) {
                myBuilder.error(message("PARSE.expected.expression"))
                return false
            }
        }
        return true
    }

    private fun parseSingleExpressionOrStringExpression() =
            if (!parseStringAdditiveExpression()) parseSingleExpression(false) else true

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

    /**
     * Parse multiline strings and method calls on strings.
     * This method is necessary as the lexer adds statement breaks,
     * preventing multiline strings from being correctly parsed by the Python parser.
     */
    private fun parseMultilineString(): Boolean {
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
        // in order for python call expression parsing to work properly
        var dotOccurred = false

        loop@while (atStringNodeOrFormattedString() || atAnyOfTokensSafe(PyTokenTypes.DOT)) {
            when {
                atAnyOfTokensSafe(PyTokenTypes.FSTRING_START) -> {
                    // will be replaced with call to Python API when it becomes public
                    parseFormattedStringNode()
                }
                // method call on a new line
                atAnyOfTokensSafe(PyTokenTypes.DOT) -> {
                    dotOccurred = true
                    statementEndPosition = myBuilder.rawTokenIndex()
                    break@loop
                }
                else -> nextToken()
            }

            // wraps the current string together with its possible indent
            incorrectUnindentMarker?.error(SnakemakeBundle.message("PARSE.incorrect.unindent"))
            incorrectUnindentMarker = null

            if (!atAnyOfTokensSafe(PyTokenTypes.STATEMENT_BREAK)) {
                when {
                    // when lines are separated with '\' symbol or there are two strings in a row like this: "1" "2"
                    atStringNodeOrFormattedString() -> continue@loop
                    atAnyOfTokensSafe(PyTokenTypes.PLUS) -> statementEndPosition = myBuilder.rawTokenIndex()
                    atAnyOfTokensSafe(PyTokenTypes.DOT) -> {
                        // call expression
                        statementEndPosition = myBuilder.rawTokenIndex()
                        dotOccurred = true
                    }
                    else -> break@loop
                }
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
                skipDedents {
                    if (incorrectUnindentMarker == null) {
                        incorrectUnindentMarker = myBuilder.mark()
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
            stringLiteralMarker.rollbackTo()
            if (statementEndPosition == -1) { // single line statement
                return parseMemberExpression(false)
            }
            stringLiteralMarker = myBuilder.mark()
            while (myBuilder.rawTokenIndex() < statementEndPosition) {
                when {
                    atStringNodeOrFormattedString() -> nextToken()
                    /*
                      atAnyOfTokens is safe to use because no keywords/anything else that is filtered
                      should appear in a string literal before a dot,
                      and we do need to skip all whitespaces up to the token,
                      not just check that the token is present but stay at the current position.
                    */
                    atAnyOfTokens(PyTokenTypes.STATEMENT_BREAK, *indentationTokenSet.types) ->
                        myBuilder.remapCurrentToken(PyTokenTypes.SPACE)

                    else -> myBuilder.advanceLexer()
                }
            }
            stringLiteralMarker.rollbackTo()
            // third pass, this time to parse everything including the dot
            return parseMemberExpression(false)
        } else if (!atAnyOfTokensSafe(PyTokenTypes.PLUS)) {
            statementBreakMarker?.rollbackTo()
        } else {
            incorrectUnindentMarker?.error(SnakemakeBundle.message("PARSE.incorrect.unindent"))
            statementBreakMarker?.drop()
        }

        indents = previousIndents
        stringLiteralMarker.done(PyElementTypes.STRING_LITERAL_EXPRESSION)
        return true
    }

    private fun parseStringAdditiveExpression(): Boolean {
        var expr = myBuilder.mark()
        if (!parseMultilineString()) {
            expr.drop()
            return false
        } else {
            while (atToken(PyTokenTypes.PLUS)) {
                myBuilder.advanceLexer()
                skipStatementBreaksUpToTokens(*stringLiteralTokenSet.types)
                if (!parseMultilineString()) {
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

    /**
     * Skips statement breaks, indents, and dedents until another token occurs.
     * If this token is one of [tokenTypes],
     * we leave the method at this token with everything up to it parsed correctly,
     * otherwise we roll back to statement break and restore the previous indent balance.
     */
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
                if (indents == 0) {
                    break
                }
            }
        }
        if (atAnyOfTokensSafe(*tokenTypes)) {
            marker.drop()
        } else {
            indents = previousIndents
            marker.rollbackTo()
        }
    }

    /**
     * Skips dedents while keeping the indent balance correct until reaches [indentLimit].
     * Incorrect dedents are handled by [incorrectUnindentHandler].
     */
    private fun skipDedents(
            indentLimit: Int = 0,
            incorrectUnindentHandler: () -> Unit
    ) {
        // IMPORTANT: note that myBuilder.eof() could apply filter to lexemes
        // which is why it must be evaluated second
        loop@ while (indents > indentLimit && !myBuilder.eof()) {
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

    private fun parseFormattedStringNode() {
        val builder = myContext.builder
        if (atToken(PyTokenTypes.FSTRING_START)) {
            val prefixThenQuotes = builder.tokenText!!
            val openingQuotes = prefixThenQuotes.replaceFirst("^[UuBbCcRrFf]*".toRegex(), "")
            val marker = builder.mark()
            nextToken()
            while (true) {
                if (atToken(PyTokenTypes.FSTRING_TEXT)) {
                    nextToken()
                } else if (atToken(PyTokenTypes.FSTRING_FRAGMENT_START)) {
                    parseFStringFragment()
                } else if (atToken(PyTokenTypes.FSTRING_END)) {
                    if (builder.tokenText == openingQuotes) {
                        nextToken()
                    } else {
                        builder.mark().error("$openingQuotes expected")
                    }// Can be the end of an enclosing f-string, so leave it in the stream
                    break
                } else if (atToken(PyTokenTypes.STATEMENT_BREAK)) {
                    builder.mark().error("$openingQuotes expected")
                    break
                } else {
                    builder.error("unexpected f-string token")
                    break
                }
            }
            marker.done(PyElementTypes.FSTRING_NODE)
        }
    }

    private fun parseFStringFragment() {
        val builder = myContext.builder
        if (atToken(PyTokenTypes.FSTRING_FRAGMENT_START)) {
            val marker = builder.mark()
            nextToken()
            var recoveryMarker: PsiBuilder.Marker = builder.mark()
            val parsedExpression = myContext.expressionParser.parseExpressionOptional()
            if (parsedExpression) {
                recoveryMarker.drop()
                recoveryMarker = builder.mark()
            }
            var recovery = !parsedExpression
            while (!builder.eof() && !atAnyOfTokens(PyTokenTypes.FSTRING_FRAGMENT_TYPE_CONVERSION,
                            PyTokenTypes.FSTRING_FRAGMENT_FORMAT_START,
                            PyTokenTypes.FSTRING_FRAGMENT_END,
                            PyTokenTypes.FSTRING_END,
                            PyTokenTypes.STATEMENT_BREAK)) {
                nextToken()
                recovery = true
            }
            if (recovery) {
                recoveryMarker.error(if (parsedExpression) "unexpected expression part" else "expression expected")
                recoveryMarker.setCustomEdgeTokenBinders(null, ExpressionParsing.CONSUME_COMMENTS_AND_SPACES_TO_LEFT)
            } else {
                recoveryMarker.drop()
            }
            val hasTypeConversion = matchToken(PyTokenTypes.FSTRING_FRAGMENT_TYPE_CONVERSION)
            val hasFormatPart = atToken(PyTokenTypes.FSTRING_FRAGMENT_FORMAT_START)
            if (hasFormatPart) {
                parseFStringFragmentFormatPart()
            }
            var errorMessage = "} expected"
            if (!hasFormatPart && !atToken(PyTokenTypes.FSTRING_END)) {
                errorMessage = ": or $errorMessage"
                if (!hasTypeConversion) {
                    errorMessage = "type conversion, $errorMessage"
                }
            }
            checkMatches(PyTokenTypes.FSTRING_FRAGMENT_END, errorMessage)
            marker.setCustomEdgeTokenBinders(null, ExpressionParsing.CONSUME_COMMENTS_AND_SPACES_TO_LEFT)
            marker.done(PyElementTypes.FSTRING_FRAGMENT)
        }
    }

    private fun parseFStringFragmentFormatPart() {
        if (atToken(PyTokenTypes.FSTRING_FRAGMENT_FORMAT_START)) {
            val marker = myContext.builder.mark()
            nextToken()
            while (true) {
                if (atToken(PyTokenTypes.FSTRING_TEXT)) {
                    nextToken()
                } else if (atToken(PyTokenTypes.FSTRING_FRAGMENT_START)) {
                    parseFStringFragment()
                } else {
                    break
                }
            }
            marker.done(PyElementTypes.FSTRING_FRAGMENT_FORMAT_PART)
        }
    }
}