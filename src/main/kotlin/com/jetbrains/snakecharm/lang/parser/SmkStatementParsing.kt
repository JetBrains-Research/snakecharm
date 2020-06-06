package com.jetbrains.snakecharm.lang.parser

import com.intellij.psi.tree.IElementType
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyPsiBundle
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.parsing.Parsing
import com.jetbrains.python.parsing.StatementParsing
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.SUBWORKFLOW_SECTIONS_KEYWORDS
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.RULE_OR_CHECKPOINT
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes.*


/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SmkStatementParsing(
        context: SmkParserContext,
        futureFlag: FUTURE?
) : StatementParsing(context, futureFlag) {

    private val ruleSectionParsingData = SectionParsingData(
            declaration = RULE_DECLARATION_STATEMENT,
            name = "rule",
            parameterListStatement = SmkElementTypes.RULE_OR_CHECKPOINT_ARGS_SECTION_STATEMENT,
            parameters = RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS,
            sectionKeyword= SmkTokenTypes.RULE_KEYWORD
    )

    private val checkpointSectionParsingData = SectionParsingData(
            declaration = CHECKPOINT_DECLARATION_STATEMENT,
            name = "checkpoint",
            parameterListStatement = SmkElementTypes.RULE_OR_CHECKPOINT_ARGS_SECTION_STATEMENT,
            parameters = RULE_OR_CHECKPOINT_ARGS_SECTION_KEYWORDS,
            sectionKeyword= SmkTokenTypes.CHECKPOINT_KEYWORD
    )

    private val subworkflowSectionParsingData = SectionParsingData(
            declaration = SUBWORKFLOW_DECLARATION_STATEMENT,
            name = "subworkflow",
            parameterListStatement = SmkElementTypes.SUBWORKFLOW_ARGS_SECTION_STATEMENT,
            parameters = SUBWORKFLOW_SECTIONS_KEYWORDS,
            sectionKeyword= SmkTokenTypes.SUBWORKFLOW_KEYWORD
    )

    override fun getReferenceType() = SmkElementTypes.SMK_PY_REFERENCE_EXPRESSION

    private fun getSectionParsingData(tokenType: IElementType) =
            when {
                tokenType === SmkTokenTypes.SUBWORKFLOW_KEYWORD -> subworkflowSectionParsingData
                tokenType === SmkTokenTypes.CHECKPOINT_KEYWORD -> checkpointSectionParsingData
                else -> ruleSectionParsingData
            }

    override fun getParsingContext() = myContext as SmkParserContext

    override fun parseStatement() {
        val context = parsingContext
        val scope = context.scope

        myBuilder.setDebugMode(false)
        if (myBuilder.tokenType == PyTokenTypes.IDENTIFIER && !scope.inPythonicSection) {
            val actualToken = SnakemakeLexer.KEYWORDS[myBuilder.tokenText!!]
            if (actualToken != null) {
                myBuilder.remapCurrentToken(actualToken)
            }
        }
        val tt = myBuilder.tokenType

        if (tt !in SmkTokenTypes.WORKFLOW_TOPLEVEL_DECORATORS) {
            super.parseStatement()
            return
        }
        when {
            tt in SmkTokenTypes.RULE_LIKE -> parseRuleLikeDeclaration(getSectionParsingData(tt!!))
            tt in SmkTokenTypes.WORKFLOW_TOPLEVEL_PARAMLISTS_DECORATOR_KEYWORDS -> {
                val workflowParam = myBuilder.mark()
                nextToken()
                parsingContext.expressionParser.parseRuleLikeSectionArgumentList()
                workflowParam.done(SmkElementTypes.WORKFLOW_ARGS_SECTION_STATEMENT)
            }
            tt === SmkTokenTypes.WORKFLOW_LOCALRULES_KEYWORD -> {
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

                workflowParam.done(SmkElementTypes.WORKFLOW_LOCALRULES_SECTION_STATEMENT)
            }
            tt === SmkTokenTypes.WORKFLOW_RULEORDER_KEYWORD  -> {
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

                workflowParam.done(SmkElementTypes.WORKFLOW_RULEORDER_SECTION_STATEMENT)
            }
            tt in SmkTokenTypes.WORKFLOW_TOPLEVEL_PYTHON_BLOCK_PARAMETER_KEYWORDS -> {
                myContext.pushScope(scope.withPythonicSection())
                val decoratorMarker = myBuilder.mark()
                nextToken()
                checkMatches(PyTokenTypes.COLON, PyPsiBundle.message("PARSE.expected.colon"))
                parseSuite()
                decoratorMarker.done(SmkElementTypes.WORKFLOW_PY_BLOCK_SECTION_STATEMENT)
                myContext.popScope()
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

    private fun parseRuleLikeDeclaration(section: SectionParsingData) {
        val ruleLikeMarker = myBuilder.mark()
        nextToken()

        // rule name
        //val ruleNameMarker: PsiBuilder.Marker = myBuilder.mark()
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

        val multiline = atToken(PyTokenTypes.STATEMENT_BREAK)
        if (!multiline) {
            parseRuleParameter(section)
        } else {
            nextToken()
            incompleteRule = !checkMatches(PyTokenTypes.INDENT, "Indent expected...")
            if (!incompleteRule) {
                while (!atToken(PyTokenTypes.DEDENT)) {
                    if (!parseRuleParameter(section)) {
                        break
                    }
                }
            }
        }

        ruleStatements.done(PyElementTypes.STATEMENT_LIST)
        ruleLikeMarker.done(section.declaration)

        when {
            incompleteRule && atAnyOfTokens(*SmkTokenTypes.RULE_LIKE.types) -> {
                // inside rule scope, we remap some snakemake keywords to identifiers
                // see #com.jetbrains.snakecharm.lang.parser.SnakemakeStatementParsing.filter
                //
                // Do nothing, next rule will be parsed automatically
                // XXX probably recover until some useful token, see recoverUntilMatches() method
                // XXX at the moment it seems any complex behaviour isn't needed
            }
            multiline && !myBuilder.eof() -> {
                if (atToken(PyTokenTypes.IDENTIFIER)) {
                    val actualToken = SnakemakeLexer.KEYWORDS[myBuilder.tokenText!!]
                    if (actualToken != null) {
                        myBuilder.remapCurrentToken(actualToken)
                        return
                    }
                }
                if (!atAnyOfTokens(*SmkTokenTypes.WORKFLOW_TOPLEVEL_DECORATORS.types)) {
                    nextToken() // probably check token type
                }
            }
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

            if (myBuilder.eof()) {
                myBuilder.error(SnakemakeBundle.message("PARSE.eof.docstring"))
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
            section.sectionKeyword in RULE_OR_CHECKPOINT && keyword == SnakemakeNames.SECTION_RUN -> {
                val scope = myContext.scope as SmkParsingScope
                myContext.pushScope(scope.withPythonicSection())
                checkMatches(PyTokenTypes.COLON, PyPsiBundle.message("PARSE.expected.colon"))
                statementParser.parseSuite()
                ruleParam.done(SmkElementTypes.RULE_OR_CHECKPOINT_RUN_SECTION_STATEMENT)
                myContext.popScope()
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

    // TODO: cleanup
//    override fun getFunctionParser(): FunctionParsing {
//        return super.getFunctionParser()
//    }


    private fun parseIdentifier(): Boolean {
        val referenceMarker = myBuilder.mark()
        if (Parsing.isIdentifier(myBuilder)) {
            Parsing.advanceIdentifierLike(myBuilder)
            referenceMarker.done(SmkElementTypes.REFERENCE_EXPRESSION)
            return true
        }
        referenceMarker.drop()
        return false
    }
}

fun IElementType?.isPythonString() = this in PyTokenTypes.STRING_NODES || this == PyTokenTypes.FSTRING_START

private data class SectionParsingData(
        val declaration: IElementType,
        val name: String,
        val parameterListStatement: PyElementType,
        val parameters: Set<String>,
        val sectionKeyword: PyElementType
)
