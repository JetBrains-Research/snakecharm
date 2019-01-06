package com.jetbrains.snakemake.lang.parser

import com.intellij.lang.PsiBuilder
import com.jetbrains.python.PyBundle
import com.jetbrains.python.PyBundle.message
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.parsing.StatementParsing
import com.jetbrains.snakemake.lang.psi.SMKRuleParameterListStatement
import com.jetbrains.snakemake.lang.psi.SMKRuleRunParameter
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

    // TODO cleanup
//    override fun getReferenceType(): IElementType {
//            return CythonElementTypes.REFERENCE_EXPRESSION
//        }

    override fun parseStatement() {
        // TODO cleanup:
//        val context = parsingContext
//        val scope = context.scope as SnakemakeParsingScope
//        var isRule = scope.isRule
        // myBuilder.setDebugMode(true)

        val tt = myBuilder.tokenType
        if (!SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_DECORATORS.contains(tt)) {
            super.parseStatement()
            return
        }
        when {
            tt === SnakemakeTokenTypes.RULE_KEYWORD -> parseRuleDeclaration(true)
            tt === SnakemakeTokenTypes.CHECKPOINT_KEYWORD -> parseRuleDeclaration(false)
            tt in SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_PARAMLISTS_DECORATORS -> {
                val workflowParam = myBuilder.mark()
                nextToken()
                checkMatches(PyTokenTypes.COLON, message("PARSE.expected.colon"))
                parsingContext.expressionParser.parseRuleParamArgumentList()
                workflowParam.done(SnakemakeElementTypes.WORKFLOW_PYTHON_BLOCK_PARAMETER)
            }
            tt === SnakemakeTokenTypes.WORKFLOW_LOCALRULES -> {
                // TODO parse local rules
                nextToken()
            }
            tt === SnakemakeTokenTypes.WORKFLOW_RULEORDER  -> {
                // TODO parse rule order
                nextToken()
            }
            tt in SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_PYTHON_BLOCK_PARAMETER -> {
                val decoratorMarker = myBuilder.mark()
                nextToken()
                checkMatches(PyTokenTypes.COLON, message("PARSE.expected.colon"))
                parseSuite()
                decoratorMarker.done(SnakemakeElementTypes.WORKFLOW_PYTHON_BLOCK_PARAMETER)
            }
            else -> {
                myBuilder.error("Unexpected token type: $tt with text: '${myBuilder.tokenText}'") // bundle
                // XXX: let's do not throw exception here parsing error is better noticeable than bg exception + doesn't
                // XXX: break PSI dramatically. But it is like assertion here.

                // fail safe:
                super.parseStatement()
            }
        }
    }

    private fun parseRuleDeclaration(atRuleToken: Boolean) {
        //            isRule = true
        val ruleMarker: PsiBuilder.Marker = myBuilder.mark()
        nextToken()

        // rule name
        if (atToken(PyTokenTypes.IDENTIFIER)) {
            nextToken()
        }
        checkMatches(PyTokenTypes.COLON, "Rule name identifier or ':' expected") // bundle
        val multiline = atToken(PyTokenTypes.STATEMENT_BREAK)
        if (!multiline) {
            parseRuleParameter()
        } else {
            nextToken()
            checkMatches(PyTokenTypes.INDENT, "Indent expected...") // bundle
            while (!atToken(PyTokenTypes.DEDENT)) {
                if (!parseRuleParameter()) {
                    break
                }
            }
        }
        ruleMarker.done(when {
            atRuleToken -> SnakemakeElementTypes.RULE_DECLARATION
            else -> SnakemakeElementTypes.CHECKPOINT_DECLARATION
        })
        if (multiline) {
            nextToken()
        }
    }

    private fun parseRuleParameter(): Boolean {
        if (myBuilder.eof()) {
            return false
        }

        val keyword = myBuilder.tokenText
        val ruleParam = myBuilder.mark()

        if (!SnakemakeTokenTypes.RULE_PARAM_IDENTIFIER_LIKE.contains(myBuilder.tokenType)) {
            myBuilder.error("Rule parameter identifier is expected") // bundle
            nextToken()
            ruleParam.drop()
            return false
        }
        nextToken()

        checkMatches(PyTokenTypes.COLON, PyBundle.message("PARSE.expected.colon"))

        var result = false

        when (keyword) {
            in SMKRuleParameterListStatement.KEYWORDS -> {
                // TODO: probably do this behaviour by default and use inspection error
                // instead of parsing errors..
                result = parsingContext.expressionParser.parseRuleParamArgumentList()
                ruleParam.done(SnakemakeElementTypes.RULE_PARAMETER_LIST_STATEMENT)
            }
            in SMKRuleRunParameter.KEYWORDS -> {
                statementParser.parseSuite()
                ruleParam.done(SnakemakeElementTypes.RULE_RUN_STATEMENT)
            }
            else -> {
                // error
                myBuilder.error("Unexpected keyword $keyword in rule definition") // bundle

                //TODO advance until eof or STATEMENT_END?
                // checkEndOfStatement()
                ruleParam.drop()
            }
        }
        return result
    }

    // TODO: cleanup
//    override fun getFunctionParser(): FunctionParsing {
//        return super.getFunctionParser()
//    }
}