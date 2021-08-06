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
import com.jetbrains.snakecharm.lang.parser.SnakemakeLexer.Companion.TOPLEVEL_KEYWORDS
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkElementTypes
import com.jetbrains.snakecharm.lang.psi.elementTypes.SmkStubElementTypes.*
import java.util.*


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
        tryRemapCurrentToken(scope, this::isToplevelDecoratorKeyword)
        val tt = myBuilder.tokenType

        if (tt !in SmkTokenTypes.WORKFLOW_TOPLEVEL_DECORATORS) {
            // XXX: maybe also allow: `some_new_section: ` case, i.e. indentifier with following ':' in order
            // to support any section here
            super.parseStatement()
            return
        }
        when {
            tt in SmkTokenTypes.RULE_LIKE -> parseRuleLikeDeclaration(getSectionParsingData(tt!!))
            tt === SmkTokenTypes.WORKFLOW_TOPLEVEL_DECORATOR_KEYWORD -> {
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
            tt === SmkTokenTypes.WORKFLOW_RULEORDER_KEYWORD -> {
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
        if (section == useSectionParsingData) {
            if (myBuilder.tokenText != SnakemakeNames.RULE_KEYWORD) {
                myBuilder.error(SnakemakeBundle.message("PARSE.use.rule.keyword.expected"))
            } else {
                myBuilder.remapCurrentToken(SmkTokenTypes.RULE_KEYWORD)
                nextToken()
            }
            // Parse rest words in 'use' definition
            val names = mutableListOf<String>()
            parseRestUseStatement(parseUseSection(names), names, section)
            ruleLikeMarker.done(section.declaration)
            return
        } else if (atToken(PyTokenTypes.IDENTIFIER)) {
            // rule name
            //val ruleNameMarker: PsiBuilder.Marker = myBuilder.mark()
            nextToken()
        }

        // XXX at the moment we continue parsing rule even if colon missed, probably better
        // XXX to drop rule and scroll up to next STATEMENT_BREAK/RULE/CHECKPOINT/other toplevel keyword or eof()
        checkMatches(PyTokenTypes.COLON,
            "${
                section.name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            } name identifier or ':' expected"
        ) // bundle

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
            myBuilder.error("${
                section.name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            } parameter identifier is expected") // bundle
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

    private inline fun tryRemapCurrentToken(scope: SmkParsingScope, checkToplevel: () -> Boolean) {
        if (myBuilder.tokenType == PyTokenTypes.IDENTIFIER && !scope.inPythonicSection) {
            val actualToken = SnakemakeLexer.KEYWORDS[myBuilder.tokenText!!]
            if (actualToken != null) {
                myBuilder.remapCurrentToken(actualToken)
            } else if (checkToplevel()) {
                myBuilder.remapCurrentToken(SmkTokenTypes.WORKFLOW_TOPLEVEL_DECORATOR_KEYWORD)
            }
        }
    }

    private fun isToplevelDecoratorKeyword() = if (myBuilder.tokenText!! in TOPLEVEL_KEYWORDS) {
        true
    } else {
        val workflowParam = myBuilder.mark()
        var result = checkNextToken(PyTokenTypes.COLON)
        nextToken()
        result = result &&
                (myBuilder.tokenType.isPythonString() || myBuilder.tokenType == PyTokenTypes.STATEMENT_BREAK)
        workflowParam.rollbackTo()
        result
    }

    private fun checkNextToken(tt: PyElementType): Boolean {
        nextToken()
        if (myBuilder.tokenType == tt) {
            return true
        }
        return false
    }

    /**
     * Parses 'use' section. If any part of 'use' section declaration is missing,
     * it will create an error message and keep parsing the section.
     * Returns true if 'as' part was detected
     */
    private fun parseUseSection(names: MutableList<String>): Boolean {
        var asKeywordExists = false

        when (myBuilder.tokenType) {
            PyTokenTypes.FROM_KEYWORD -> myBuilder.apply {
                error(SnakemakeBundle.message("PARSE.use.names.expected"))
                asKeywordExists = fromSignatureParsing(names)
            }
            PyTokenTypes.AS_KEYWORD -> myBuilder.apply {
                asKeywordExists = true
                error(SnakemakeBundle.message("PARSE.use.names.expected"))
                asSignatureParsing(names)
            }
            PyTokenTypes.IDENTIFIER -> {
                val marker = myBuilder.mark()
                parseIdentifierFromIdentifiersList(names)
                marker.done(SmkElementTypes.USE_IMPORTED_RULES_NAMES)
                asKeywordExists = endOfImportedRulesDeclaration(false, names)
            }
            PyTokenTypes.MULT -> {
                val marker = myBuilder.mark()
                names.add(myBuilder.tokenText!!)
                nextToken()
                if (atToken(PyTokenTypes.COMMA)) {
                    myBuilder.error(SnakemakeBundle.message("PARSE.use.wildcard.in.names.list"))
                    nextToken()
                    parseIdentifierFromIdentifiersList(names)
                }
                marker.done(SmkElementTypes.USE_IMPORTED_RULES_NAMES)
                asKeywordExists = endOfImportedRulesDeclaration(true, names)
            }
            else -> myBuilder.error(SnakemakeBundle.message("PARSE.use.names.expected"))
        }

        return asKeywordExists
    }

    /**
     * Uses when we need to parse list of imported rules names.
     */
    private fun parseIdentifierFromIdentifiersList(
        list: MutableList<String>
    ) {
        var hasNext = true
        while (hasNext) {
            hasNext = when (myBuilder.tokenType) {
                PyTokenTypes.IDENTIFIER -> {
                    val referenceMarker = myBuilder.mark() // Register new name
                    list.add(myBuilder.tokenText ?: return)
                    Parsing.advanceIdentifierLike(myBuilder)
                    referenceMarker.done(SmkElementTypes.REFERENCE_EXPRESSION)
                    registerCommaOrEndOfNames()
                }
                PyTokenTypes.MULT -> {
                    myBuilder.error(SnakemakeBundle.message("PARSE.use.wildcard.in.names.list"))
                    nextToken()
                    registerCommaOrEndOfNames()
                }
                PyTokenTypes.COMMA -> {
                    myBuilder.error(SnakemakeBundle.message("PARSE.use.names.expected"))
                    nextToken()
                    true
                }
                // Actually, Snakemake allows any name for rules and modules,
                // that have python token type NAME, which may contains such words as:
                // 'use', 'as', 'from'
                else -> {
                    myBuilder.error(SnakemakeBundle.message("PARSE.use.names.expected"))
                    false
                }
            }
        }
    }

    /**
     * Uses when we need to register comma in list of imported rules names
     * or ending of the list
     */
    private fun registerCommaOrEndOfNames(): Boolean = when (myBuilder.tokenType) {
        PyTokenTypes.FROM_KEYWORD, PyTokenTypes.AS_KEYWORD, PyTokenTypes.WITH_KEYWORD -> false
        PyTokenTypes.COMMA -> {
            nextToken()
            true
        }
        else -> {
            myBuilder.error(SnakemakeBundle.message("PARSE.use.unexpected.names.separator"))
            false
        }
    }

    /**
     * Uses when we've finished to collect rules names and went to the next step
     * Returns true if 'as' part was detected
     */
    private fun endOfImportedRulesDeclaration(
        definedByWildcard: Boolean,
        names: List<String>
    ): Boolean = when (myBuilder.tokenType) {
        PyTokenTypes.FROM_KEYWORD -> fromSignatureParsing(names)
        PyTokenTypes.AS_KEYWORD -> {
            if (definedByWildcard) {
                myBuilder.error(SnakemakeBundle.message("PARSE.use.unexpected.list.ending"))
            }
            if (names.size > 1) {
                myBuilder.error(SnakemakeBundle.message("PARSE.use.few.names.from.current.module"))
            }
            asSignatureParsing(names)
            true
        }
        else -> {
            myBuilder.error(SnakemakeBundle.message("PARSE.use.unexpected.list.ending"))
            false
        }
    }

    /**
     * Parses 'from' part in 'use' section declaration.
     * Returns true if 'as' part was detected after 'from' part
     */
    private fun fromSignatureParsing(names: List<String>): Boolean {
        myBuilder.remapCurrentToken(SmkTokenTypes.SMK_FROM_KEYWORD)
        nextToken()

        if (!atToken(PyTokenTypes.IDENTIFIER)) {
            myBuilder.error(SnakemakeBundle.message("PARSE.use.expecting.module.name"))
        } else {
            parseIdentifier()
        }

        if (atToken(PyTokenTypes.AS_KEYWORD)) {
            asSignatureParsing(names)
            return true
        }

        return false
    }

    /**
     * Parses 'as' part in 'use' section declaration
     */
    private fun asSignatureParsing(names: List<String>) {
        myBuilder.remapCurrentToken(SmkTokenTypes.SMK_AS_KEYWORD)
        nextToken()

        var lasTokenIsIdentifier =
            !atToken(PyTokenTypes.IDENTIFIER) // Default value need to ve reversed
        var simpleName = true // Does new rule name consist of one identifier
        var hasIdentifier = false // Do we have new rule name
        val name = myBuilder.mark()
        while (true) {
            when (myBuilder.tokenType) {
                PyTokenTypes.IDENTIFIER -> {
                    if (lasTokenIsIdentifier) {
                        break // Because it's separated by whitespace so it isn't name anymore
                    }
                    lasTokenIsIdentifier = true
                    hasIdentifier = true
                    nextToken()
                }
                PyTokenTypes.MULT -> {
                    if (!lasTokenIsIdentifier) {
                        myBuilder.error(SnakemakeBundle.message("PARSE.use.double.mult.sign"))
                    }
                    lasTokenIsIdentifier = false
                    hasIdentifier = true
                    simpleName = false
                    nextToken()
                }
                PyTokenTypes.EXP, PyTokenTypes.COMMA -> {
                    myBuilder.error(SnakemakeBundle.message("PARSE.use.double.mult.sign"))
                    lasTokenIsIdentifier = false
                    simpleName = false
                    nextToken()
                }
                else -> break
            }
        }
        if (!hasIdentifier) { // No identifiers and/or '*' symbols
            name.drop()
            // Currently (6.5.3) it's ok for snakemake
            // We can write 'as with:' or just 'as' and end line
            // Both variants are allowed and original rule names will be taken
        } else {
            if (!simpleName) { // New rule name contains at least one '*' symbol
                name.done(SmkElementTypes.USE_NAME_IDENTIFIER)
            } else { // New rule name consists of one identifier
                name.drop()
                if (names.size > 1) {
                    myBuilder.error(SnakemakeBundle.message("PARSE.use.requires.wildcard"))
                }
            }
        }
    }

    /**
     * Parses end of 'use' section declaration and its arguments sections
     */
    private fun parseRestUseStatement(
        asKeywordExists: Boolean,
        names: List<String>,
        section: SectionParsingData
    ) {
        // Parses 'with' part
        val argsSectionsBanned = (names.size == 1 && names[0] == "*")
        var gotWithOrColon = false

        if (atToken(PyTokenTypes.WITH_KEYWORD)) {
            gotWithOrColon = true
            if (argsSectionsBanned) {
                myBuilder.error(SnakemakeBundle.message("PARSE.use.with.not.allowed"))
            }
            myBuilder.remapCurrentToken(SmkTokenTypes.SMK_WITH_KEYWORD)
            nextToken()
        }

        if (atToken(PyTokenTypes.COLON)) {
            if (!gotWithOrColon) {
                myBuilder.error(SnakemakeBundle.message("PARSE.use.with.missed"))
            }
            gotWithOrColon = true
            nextToken()
        } else if (gotWithOrColon) {
            myBuilder.error(SnakemakeBundle.message("PARSE.use.expecting.colon"))
        }

        // Parses arguments sections
        if (myBuilder.tokenType.isPythonString()) {
            parsingContext.expressionParser.parseStringLiteralExpression()
        }
        if (!atToken(PyTokenTypes.STATEMENT_BREAK)) {
            val ruleStatements = myBuilder.mark()
            verifyArgsSections(argsSectionsBanned, gotWithOrColon, asKeywordExists)
            parseRuleParameter(section)
            ruleStatements.done(PyElementTypes.STATEMENT_LIST)
            return
        }
        val ruleStatements = myBuilder.mark()
        nextToken()

        if (!atToken(PyTokenTypes.INDENT)) {
            ruleStatements.done(PyElementTypes.STATEMENT_LIST)
            // No error if we got 'with:' without arguments sections
            return
        }

        verifyArgsSections(argsSectionsBanned, gotWithOrColon, asKeywordExists)

        nextToken()
        while (!atToken(PyTokenTypes.DEDENT)) {
            if (!parseRuleParameter(section)) {
                break
            }
        }
        ruleStatements.done(PyElementTypes.STATEMENT_LIST)
        nextToken()
    }

    private fun verifyArgsSections(argsSectionsBanned: Boolean, gotWithOrColon: Boolean, asKeywordExists: Boolean) {
        if (argsSectionsBanned && !gotWithOrColon) {
            myBuilder.error(SnakemakeBundle.message("PARSE.use.args.sections.forbidden"))
        }

        if (!gotWithOrColon && !asKeywordExists) {
            myBuilder.error(SnakemakeBundle.message("PARSE.use.as.or.with.expecting"))
        } else if (!gotWithOrColon) {
            myBuilder.error(SnakemakeBundle.message("PARSE.use.with.expecting"))
        }
    }
}

fun IElementType?.isPythonString() = this in PyTokenTypes.STRING_NODES || this == PyTokenTypes.FSTRING_START

private data class SectionParsingData(
    val declaration: IElementType,
    val name: String,
    val parameterListStatement: PyElementType,
    val sectionKeyword: PyElementType
)
