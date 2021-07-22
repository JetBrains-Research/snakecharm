package com.jetbrains.snakecharm.lang.parser

import com.intellij.psi.tree.IElementType
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyPsiBundle
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.parsing.Parsing
import com.jetbrains.python.parsing.StatementParsing
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakecharm.SnakemakeBundle
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.RULE_OR_CHECKPOINT
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes.*


/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SmkStatementParsing(
    context: SmkParserContext
) : StatementParsing(context) {

    private val ruleSectionParsingData = SectionParsingData(
        declaration = RULE_DECLARATION_STATEMENT,
        name = "rule",
        parameterListStatement = SmkElementTypes.RULE_OR_CHECKPOINT_ARGS_SECTION_STATEMENT,
        sectionKeyword = SmkTokenTypes.RULE_KEYWORD
    )

    private val checkpointSectionParsingData = SectionParsingData(
        declaration = CHECKPOINT_DECLARATION_STATEMENT,
        name = "checkpoint",
        parameterListStatement = SmkElementTypes.RULE_OR_CHECKPOINT_ARGS_SECTION_STATEMENT,
        sectionKeyword = SmkTokenTypes.CHECKPOINT_KEYWORD
    )

    private val subworkflowSectionParsingData = SectionParsingData(
        declaration = SUBWORKFLOW_DECLARATION_STATEMENT,
        name = "subworkflow",
        parameterListStatement = SmkElementTypes.SUBWORKFLOW_ARGS_SECTION_STATEMENT,
        sectionKeyword = SmkTokenTypes.SUBWORKFLOW_KEYWORD
    )

    private val moduleSectionParsingData = SectionParsingData(
        declaration = MODULE_DECLARATION_STATEMENT,
        name = "module",
        parameterListStatement = SmkElementTypes.MODULE_ARGS_SECTION_STATEMENT,
        sectionKeyword = SmkTokenTypes.MODULE_KEYWORD
    )

    private val useSectionParsingData = SectionParsingData(
        declaration = USE_DECLARATION_STATEMENT,
        name = "use",
        parameterListStatement = SmkElementTypes.USE_ARGS_SECTION_STATEMENT,
        sectionKeyword = SmkTokenTypes.USE_KEYWORD
    )

    override fun getReferenceType() = SmkElementTypes.SMK_PY_REFERENCE_EXPRESSION

    private fun getSectionParsingData(tokenType: IElementType) =
        when {
            tokenType === SmkTokenTypes.SUBWORKFLOW_KEYWORD -> subworkflowSectionParsingData
            tokenType === SmkTokenTypes.CHECKPOINT_KEYWORD -> checkpointSectionParsingData
            tokenType === SmkTokenTypes.MODULE_KEYWORD -> moduleSectionParsingData
            tokenType === SmkTokenTypes.USE_KEYWORD -> useSectionParsingData
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
            // XXX: maybe also allow: `some_new_section: ` case, i.e. indentifier with following ':' in order
            // to support any section here
            super.parseStatement()
            return
        }
        when {
            tt in SmkTokenTypes.RULE_LIKE -> parseRuleLikeDeclaration(getSectionParsingData(tt!!))
            tt in SmkTokenTypes.WORKFLOW_TOPLEVEL_PARAMLISTS_DECORATOR_KEYWORDS -> {
                val workflowParam = myBuilder.mark()
                nextToken()
                val result = parsingContext.expressionParser.parseRuleLikeSectionArgumentList()
                workflowParam.done(SmkElementTypes.WORKFLOW_ARGS_SECTION_STATEMENT)
                if (result) {
                    nextToken()
                }
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
                if (res) {
                    nextToken()
                }
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
                if (res) {
                    nextToken()
                }
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

        // Parse second word in 'use rule'
        if (section.sectionKeyword == SmkTokenTypes.USE_KEYWORD) {
            if (myBuilder.tokenText != SnakemakeNames.RULE_KEYWORD) {
                myBuilder.error("Unexpected token '${myBuilder.tokenText}' after 'use' keyword")
            } else {
                myBuilder.remapCurrentToken(SmkTokenTypes.RULE_KEYWORD)
                nextToken()
            }
            // Parse rest words in 'use' definition
            if (!parseUseDeclaration()) {
                myBuilder.mark().done(PyElementTypes.STATEMENT_LIST) // Child must not be null
                ruleLikeMarker.done(section.declaration)
                checkMatches(PyTokenTypes.STATEMENT_BREAK, PyPsiBundle.message("PARSE.expected.statement.break"))
                return
            }
        } else if (atToken(PyTokenTypes.IDENTIFIER)) {
            // rule name
            //val ruleNameMarker: PsiBuilder.Marker = myBuilder.mark()
            nextToken()
        }

        // XXX at the moment we continue parsing rule even if colon missed, probably better
        // XXX to drop rule and scroll up to next STATEMENT_BREAK/RULE/CHECKPOINT/other toplevel keyword or eof()
        checkMatches(PyTokenTypes.COLON, "${section.name.capitalize()} name identifier or ':' expected") // bundle

        val ruleStatements = myBuilder.mark()

        // Skipping a docstring
        if (myBuilder.tokenType.isPythonString()) {
            parsingContext.expressionParser.parseStringLiteralExpression()
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
            parsingContext.expressionParser.parseStringLiteralExpression()

            if (!atToken(PyTokenTypes.IDENTIFIER)) {
                // section expected on next line
                if (!atToken(PyTokenTypes.STATEMENT_BREAK)) {
                    val errorMarker = myBuilder.mark()
                    while (!atToken(PyTokenTypes.STATEMENT_BREAK)) {
                        nextToken()
                    }
                    errorMarker.error(SnakemakeBundle.message("PARSE.rule.expected.rule.commend.to.docstring"))
                }
                nextToken()
            } // else docstring on same line as next section

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
            section.sectionKeyword in RULE_OR_CHECKPOINT && keyword == SnakemakeNames.SECTION_RUN -> {
                val scope = myContext.scope as SmkParsingScope
                myContext.pushScope(scope.withPythonicSection())
                checkMatches(PyTokenTypes.COLON, PyPsiBundle.message("PARSE.expected.colon"))
                statementParser.parseSuite()
                ruleParam.done(SmkElementTypes.RULE_OR_CHECKPOINT_RUN_SECTION_STATEMENT)
                myContext.popScope()
            }
            else -> {
                // Snakemake often adds new sections => let's by default allow all here +
                //  show inspection error for keyword not in `section.parameters` instead of parsing errors..
                result = parsingContext.expressionParser.parseRuleLikeSectionArgumentList()
                ruleParam.done(section.parameterListStatement)
                if (result) {
                    nextToken()
                }
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

    /**
     * Parsing 'use' statement. Starts after first identifier and ends before the colon
     */
    private fun parseUseDeclaration(): Boolean {
        val listOfRules = myBuilder.tokenType == PyTokenTypes.MULT
        if (!listOfRules && myBuilder.tokenType != PyTokenTypes.IDENTIFIER) { // No rule name, no '*'
            myBuilder.error(PyPsiBundle.message("PARSE.expected.symbols.first.quotation", "*", "rule name"))
        } else if (!listOfRules) { // Have rule name, so mark it as reference
            parseIdentifier()
        } else { // Have '*'
            nextToken()
        }

        if (myBuilder.tokenType != PyTokenTypes.AS_KEYWORD) {
            checkMatches(PyTokenTypes.FROM_KEYWORD, PyPsiBundle.message("PARSE.0.expected", "from"))
            if (!parseIdentifier()) {
                myBuilder.error(PyPsiBundle.message("PARSE.expected.identifier"))
            }
        }
        checkMatches(PyTokenTypes.AS_KEYWORD, PyPsiBundle.message("PARSE.0.expected", "as"))

        // New rule name can be: text_*, *_text, text_*_text, text or *
        val hasFirstIdentifier = myBuilder.tokenType == PyTokenTypes.IDENTIFIER
        val name = myBuilder.mark()
        if (hasFirstIdentifier) {
            nextToken()
        }
        if (myBuilder.tokenType == PyTokenTypes.MULT) {
            nextToken()
            if (myBuilder.tokenType == PyTokenTypes.IDENTIFIER) {
                nextToken()
            }
            name.done(SmkElementTypes.USE_NAME_IDENTIFIER)
        } else {
            name.drop()
            if (!hasFirstIdentifier) {
                checkMatches(PyTokenTypes.IDENTIFIER, PyPsiBundle.message("PARSE.expected.identifier"))
            }
        }

        if (myBuilder.tokenType == PyTokenTypes.WITH_KEYWORD) {
            if (listOfRules) {
                myBuilder.error(SnakemakeBundle.message("PARSE.use.with.not.allowed"))
            }
            nextToken()
            return true
        }
        return false
    }
}

fun IElementType?.isPythonString() = this in PyTokenTypes.STRING_NODES || this == PyTokenTypes.FSTRING_START

private data class SectionParsingData(
    val declaration: IElementType,
    val name: String,
    val parameterListStatement: PyElementType,
    val sectionKeyword: PyElementType
)
