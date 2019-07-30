package com.jetbrains.snakecharm.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.openapi.progress.ProcessCanceledException
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
        var afterArgumentMarker: PsiBuilder.Marker? = null
        while (!myBuilder.eof()) {
            if (atToken(PyTokenTypes.STATEMENT_BREAK)) {
                nextToken()
                /* It's important to use rawLookup() inside atAnyOfTokensSafe() here
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
                    if (atToken(PyTokenTypes.IDENTIFIER)) {
                        // keyword argument
                        if (!matchToken(PyTokenTypes.EQ))  {
                            val actualToken = SnakemakeLexer.KEYWORDS[myBuilder.tokenText]
                            if (actualToken != null && indents < 1) {
                                // we have encountered a toplevel keyword, which means we are no longer inside an argument section
                                myBuilder.remapCurrentToken(actualToken)
                                afterArgumentMarker?.rollbackTo()
                                break
                            }
                        }
                    }

                    recoverUntilMatches(
                            SnakemakeBundle.message("PARSE.expected.separator.message", separatorTokenText),
                            separatorTokenType,
                            PyTokenTypes.STATEMENT_BREAK
                    )

                    if (atAnyOfTokens(*SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_DECORATORS.types)) {
                        break
                    }
                }
            }

            parseArgumentFunction()

            if (atToken(PyTokenTypes.IDENTIFIER) && afterArgumentMarker != null) {
                afterArgumentMarker = myBuilder.mark()
            }

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

            if (myBuilder.lookAhead(1) == PyTokenTypes.IDENTIFIER) {
                nextToken()
                val actualToken = SnakemakeLexer.KEYWORDS[myBuilder.tokenText]
                if (actualToken != null) {
                    myBuilder.remapCurrentToken(actualToken)
                    break
                }
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
}