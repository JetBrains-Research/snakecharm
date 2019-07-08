package com.jetbrains.snakecharm.lang.parser

import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import com.jetbrains.python.PyBundle
import com.jetbrains.python.PyBundle.message
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.parsing.Parsing
import com.jetbrains.python.parsing.StatementParsing
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.parser.SnakemakeTokenTypes.RULE_OR_CHECKPOINT
import com.jetbrains.snakecharm.lang.psi.SMKRuleParameterListStatement
import com.jetbrains.snakecharm.lang.psi.SMKRuleRunParameter
import com.jetbrains.snakecharm.lang.psi.SMKSubworkflowParameterListStatement
import com.jetbrains.snakecharm.lang.psi.elementTypes.SnakemakeElementTypes


/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeStatementParsing(
        context: SnakemakeParserContext,
        futureFlag: FUTURE?
) : StatementParsing(context, futureFlag) {

    private data class SectionParsingData(
            val declaration: PyElementType,
            val name: String,
            val parameterListStatement: PyElementType,
            val parameters: Set<String>,
            val sectionKeyword: PyElementType)


    private val ruleSectionParsingData = SectionParsingData(
            declaration = SnakemakeElementTypes.RULE_DECLARATION,
            name = "rule",
            parameterListStatement = SnakemakeElementTypes.RULE_PARAMETER_LIST_STATEMENT,
            parameters = SMKRuleParameterListStatement.PARAMS_NAMES,
            sectionKeyword= SnakemakeTokenTypes.RULE_KEYWORD
    )

    private val checkpointSectionParsingData = SectionParsingData(
            declaration = SnakemakeElementTypes.CHECKPOINT_DECLARATION,
            name = "checkpoint",
            parameterListStatement = SnakemakeElementTypes.RULE_PARAMETER_LIST_STATEMENT,
            parameters = SMKRuleParameterListStatement.PARAMS_NAMES,
            sectionKeyword= SnakemakeTokenTypes.CHECKPOINT_KEYWORD
    )

    private val subworkflowSectionParsingData = SectionParsingData(
            declaration = SnakemakeElementTypes.SUBWORKFLOW_DECLARATION,
            name = "subworkflow",
            parameterListStatement = SnakemakeElementTypes.SUBWORKFLOW_PARAMETER_LIST_STATEMENT,
            parameters = SMKSubworkflowParameterListStatement.PARAMS_NAMES,
            sectionKeyword= SnakemakeTokenTypes.SUBWORKFLOW_KEYWORD
    )

    private fun getSectionParsingData(tokenType: IElementType) =
            when {
                tokenType === SnakemakeTokenTypes.SUBWORKFLOW_KEYWORD -> subworkflowSectionParsingData
                tokenType === SnakemakeTokenTypes.CHECKPOINT_KEYWORD -> checkpointSectionParsingData
                else -> ruleSectionParsingData
            }

    override fun getParsingContext() = myContext as SnakemakeParserContext

    // TODO cleanup
    //    override fun getReferenceType(): IElementType {
    //            return CythonElementTypes.REFERENCE_EXPRESSION
    //        }

    override fun parseStatement() {
        val context = parsingContext
        val scope = context.scope

        myBuilder.setDebugMode(true)

        val tt = myBuilder.tokenType

        if (tt !in SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_DECORATORS || scope.inParamArgsList) {
            super.parseStatement()
            // TODO: context?
            return
        }
        when {
            tt in SnakemakeTokenTypes.RULE_LIKE -> parseRuleDeclaration(getSectionParsingData(tt!!))
            tt in SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_PARAMLISTS_DECORATOR_KEYWORDS -> {
                val workflowParam = myBuilder.mark()
                nextToken()
                parsingContext.expressionParser.parseRuleLikeSectionArgumentList()
                workflowParam.done(SnakemakeElementTypes.WORKFLOW_PARAMETER_LIST_STATEMENT)
            }
            tt === SnakemakeTokenTypes.WORKFLOW_LOCALRULES_KEYWORD -> {
                val workflowParam = myBuilder.mark()
                nextToken()

                val res = parsingContext.expressionParser.parseArgumentList(
                        ",", PyTokenTypes.COMMA,
                        SnakemakeBundle.message("PARSE.expected.identifier"),
                        this::parseIdentifier
                )

                if (!res) {
                    myBuilder.error(SnakemakeBundle.message("PARSE.expected.localrules"))
                }

                workflowParam.done(SnakemakeElementTypes.WORKFLOW_LOCALRULES_STATEMENT)
            }
            tt === SnakemakeTokenTypes.WORKFLOW_RULEORDER_KEYWORD  -> {
                val workflowParam = myBuilder.mark()
                nextToken()

                val res = parsingContext.expressionParser.parseArgumentList(
                        ">", PyTokenTypes.GT,
                        SnakemakeBundle.message("PARSE.expected.identifier"),
                        this::parseIdentifier
                )
                if (!res) {
                    myBuilder.error(SnakemakeBundle.message("PARSE.expected.ruleorder"))
                }

                workflowParam.done(SnakemakeElementTypes.WORKFLOW_RULEORDER_STATEMENT)
            }
            tt in SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_PYTHON_BLOCK_PARAMETER_KEYWORDS -> {
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

    private fun parseRuleDeclaration(section: SectionParsingData) {
        val context = parsingContext
        val scope = context.scope

        val ruleMarker: PsiBuilder.Marker = myBuilder.mark()
        nextToken()

        // rule name
        if (atToken(PyTokenTypes.IDENTIFIER)) {
            nextToken()
        }

        // XXX at the moment we continue parsing rule even if colon missed, probably better
        // XXX to drop rule and scroll up to next STATEMENT_BREAK/RULE/CHECKPOINT/other toplevel keyword or eof()
        checkMatches(PyTokenTypes.COLON, "${section.name.capitalize()} name identifier or ':' expected") // bundle
        val ruleStatements = myBuilder.mark()

        // Skipping a docstring
        if (myBuilder.tokenType.isPythonString()) {
            parsingContext.expressionParser.parseExpression()
        }

        // Wile typing new rule at some moment is could be incomplete, e.g. missing indent and
        // no sections after current rule before next rule
        var incompleteRule = false

        // in rule scopes helps to parse toplevel keywords as identifiers
        // see #com.jetbrains.snakecharm.lang.parser.SnakemakeStatementParsing.filter
        var inRuleScope: SnakemakeParsingScope? = scope.withRule()

        val multiline = atToken(PyTokenTypes.STATEMENT_BREAK)
        if (!multiline) {
            context.pushScope(inRuleScope!!)
            parseRuleParameter(section)
        } else {
            nextToken()
            incompleteRule = !checkMatches(PyTokenTypes.INDENT, "Indent expected...")
            if (incompleteRule) {
                inRuleScope = null
            } else {
                context.pushScope(inRuleScope!!)
                while (!atToken(PyTokenTypes.DEDENT)) {
                    if (!parseRuleParameter(section)) {
                        break
                    }
                }
            }
        }

        if (inRuleScope != null) {
            context.popScope()
        }

        ruleStatements.done(PyElementTypes.STATEMENT_LIST)
        ruleMarker.done(section.declaration)

        if (incompleteRule && atAnyOfTokens(*SnakemakeTokenTypes.RULE_LIKE.types)) {
            // inside rule scope, we remap some snakemake keywords to identifiers
            // see #com.jetbrains.snakecharm.lang.parser.SnakemakeStatementParsing.filter
            //
            // Do nothing, next rule will be parsed automatically
            // XXX probably recover until some useful token, see recoverUntilMatches() method
            // XXX at the moment it seems any complex behaviour isn't needed
        } else if (multiline && !myBuilder.eof()) {
            nextToken() // probably check toke type
        }
    }

    private fun parseRuleParameter(section: SectionParsingData): Boolean {
        if (myBuilder.eof()) {
            return false
        }

        // Skipping a docstring
        if (myBuilder.tokenType.isPythonString()) {
            parsingContext.expressionParser.parseExpression()

            if (myBuilder.tokenType === PyTokenTypes.STATEMENT_BREAK) {
                nextToken()
            }

            return true
        }

        val keyword = myBuilder.tokenText
        val ruleParam = myBuilder.mark()

        if (myBuilder.tokenType != PyTokenTypes.IDENTIFIER) {
            myBuilder.error("${section.name.capitalize()} parameter identifier is expected") // bundle
            nextToken()
            ruleParam.drop()
            return false
        }
        nextToken()

        var result = false

        when {
            keyword in section.parameters -> {
                // TODO: probably do this parsing behaviour by default and show inspection error
                // for keyword not in `section.parameters` instead of parsing errors..
                result = parsingContext.expressionParser.parseRuleLikeSectionArgumentList()
                ruleParam.done(section.parameterListStatement)
            }
            section.sectionKeyword in RULE_OR_CHECKPOINT && keyword == SMKRuleRunParameter.PARAM_NAME -> {
                checkMatches(PyTokenTypes.COLON, PyBundle.message("PARSE.expected.colon"))
                statementParser.parseSuite()
                ruleParam.done(SnakemakeElementTypes.RULE_RUN_STATEMENT)
            }
            else -> {
                // error
                myBuilder.error("Unexpected ${section.name} parameter '$keyword'") // bundle

                //TODO advance until eof or STATEMENT_END?
                // checkEndOfStatement()
                ruleParam.drop()
            }
        }

        return result
    }

    override fun filter(
            source: IElementType,
            start: Int, end: Int,
            text: CharSequence,
            checkLanguageLevel: Boolean
    ): IElementType {
        if (source in SnakemakeTokenTypes.WORKFLOW_TOPLEVEL_DECORATORS) {
            val scope = myContext.scope as SnakemakeParsingScope
            return when {
                //XXX: breaks rule keyword!!!!
                scope.inRuleSectionsList -> PyTokenTypes.IDENTIFIER
                else -> source
            }
        }
        return super.filter(source, start, end, text, checkLanguageLevel)
    }
    // TODO: cleanup
//    override fun getFunctionParser(): FunctionParsing {
//        return super.getFunctionParser()
//    }


    private fun parseIdentifier(): Boolean {
        val referenceMarker = myBuilder.mark()
        if (Parsing.isIdentifier(myBuilder)) {
            Parsing.advanceIdentifierLike(myBuilder)
            referenceMarker.done(SnakemakeElementTypes.RULE_REFERENCE)
            return true
        }
        referenceMarker.drop()
        return false
    }
}

fun IElementType?.isPythonString() : Boolean {
    return this === PyTokenTypes.TRIPLE_QUOTED_STRING ||
            this === PyTokenTypes.SINGLE_QUOTED_STRING
}