package com.jetbrains.snakecharm.lang.parser

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.intellij.psi.tree.IElementType
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.lexer.PythonIndentingLexer
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakecharm.codeInsight.SnakemakeAPI.TOPLEVEL_ARGS_SECTION_KEYWORDS
import com.jetbrains.snakecharm.lang.SnakemakeNames
import com.jetbrains.snakecharm.lang.parser.SmkTokenTypes.WORKFLOW_TOPLEVEL_ARGS_SECTION_KEYWORD

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeLexer : PythonIndentingLexer() {
    // number of spaces between line start and the first non-whitespace token on the line
    private var myCurrentNewlineIndent = 0

    // end offset of the last line break before the first non-whitespace token on the line,
    // which is also start offset of the first non-whitespace token on the line
    private var myCurrentNewlineOffset = 0

    private var currentToken: IElementType? = null
    private var previousToken: IElementType? = null
    private var insertedIndentsCount = 0
    private var linebreakBeforeFirstComment = -1

    // used to identify a potential rule section in cases like `rule [name]: section`
    // this is used only for sections which can have subsections
    // and only up until the first non-identifier, non-colon or non-whitespace token
    private var expectingRuleSectionOnSameLine = false
    private val tokensBeforeRuleSection = mutableListOf<IElementType>()

    // used to insert statement break before the first argument but only line breaks between section arguments
    private var beforeFirstArgumentInSection = false

    /*
     The following tokens can be considered top-level sections:
     1. text is present in the KEYWORDS map
     2. text is immediately followed by a colon or a whitespace character, an identifier and a colon
     3. topLevelSectionIndent is equal to -1, meaning there is no top-level section nesting the current section
    */
    private var topLevelSectionIndent = -1

    /*
     The following tokens can be considered rule-like sections:
     0. identifiers
     1. topLevelSectionIndent is greater than -1, meaning the current section is nested inside a top-level section
     2. there is a colon token following this token immediately
     Should always be not less than topLevelSectionIndent
    */
    private var ruleLikeSectionIndent = -1

    /*
     Is true for:
      - `onsuccess`/`onerror`/`onstart` top-level sections
        (and topLevelSectionIndent is greater than -1, ruleLikeSectionIndent is equal to -1)
      - `run` rule-like section (both variables above are greater than -1)
     Is not true for:
      - python toplevel code, including conditional statements, loops and the like, because that's not a section
    */
    private var isInPythonSection = true

    // Is false for: rule, checkpoint, subworkflow, true for other toplevel sections, e.g. include
    private var isInToplevelSectionWithoutSubsections = false

    companion object {
        val RULE_LIKE_KEYWORDS = ImmutableSet.Builder<String>()
            .add(SnakemakeNames.RULE_KEYWORD)
            .add(SnakemakeNames.CHECKPOINT_KEYWORD)
            .add(SnakemakeNames.SUBWORKFLOW_KEYWORD)
            .add(SnakemakeNames.MODULE_KEYWORD)
            .add(SnakemakeNames.USE_KEYWORD)
            .build()!!

        val PYTHON_BLOCK_KEYWORDS = ImmutableSet.Builder<String>()
            .add(SnakemakeNames.WORKFLOW_ONSTART_KEYWORD)
            .add(SnakemakeNames.WORKFLOW_ONSUCCESS_KEYWORD)
            .add(SnakemakeNames.WORKFLOW_ONERROR_KEYWORD)
            .build()!!

        private val SPECIAL_KEYWORDS_2_TOKEN_TYPE = ImmutableMap.Builder<String, PyElementType>()
            .put(SnakemakeNames.RULE_KEYWORD, SmkTokenTypes.RULE_KEYWORD)
            .put(SnakemakeNames.CHECKPOINT_KEYWORD, SmkTokenTypes.CHECKPOINT_KEYWORD)
            .put(SnakemakeNames.WORKFLOW_LOCALRULES_KEYWORD, SmkTokenTypes.WORKFLOW_LOCALRULES_KEYWORD)
            .put(SnakemakeNames.WORKFLOW_RULEORDER_KEYWORD, SmkTokenTypes.WORKFLOW_RULEORDER_KEYWORD)
            .put(SnakemakeNames.WORKFLOW_ONSUCCESS_KEYWORD, SmkTokenTypes.WORKFLOW_ONSUCCESS_KEYWORD)
            .put(SnakemakeNames.WORKFLOW_ONERROR_KEYWORD, SmkTokenTypes.WORKFLOW_ONERROR_KEYWORD)
            .put(SnakemakeNames.WORKFLOW_ONSTART_KEYWORD, SmkTokenTypes.WORKFLOW_ONSTART_KEYWORD)
            .put(SnakemakeNames.SUBWORKFLOW_KEYWORD, SmkTokenTypes.SUBWORKFLOW_KEYWORD)
            .put(SnakemakeNames.MODULE_KEYWORD, SmkTokenTypes.MODULE_KEYWORD)
            .put(SnakemakeNames.USE_KEYWORD, SmkTokenTypes.USE_KEYWORD)
            .build()!!


        val KEYWORD_LIKE_SECTION_TOKEN_TYPE_2_KEYWORD: Map<PyElementType, String?> =
            SPECIAL_KEYWORDS_2_TOKEN_TYPE.map { it.value to it.key }.toMap() +
                    // multiple section names are possible, no mapping:
                    listOf(WORKFLOW_TOPLEVEL_ARGS_SECTION_KEYWORD to null).toMap()

        val KEYWORD_LIKE_SECTION_NAME_2_TOKEN_TYPE =
            TOPLEVEL_ARGS_SECTION_KEYWORDS.associateWith { WORKFLOW_TOPLEVEL_ARGS_SECTION_KEYWORD } + SPECIAL_KEYWORDS_2_TOKEN_TYPE
    }

    override fun advance() {
        if (expectingRuleSectionOnSameLine) {
            when {
                tokenType == PyTokenTypes.COLON || tokenType in PyTokenTypes.WHITESPACE ->
                    tokensBeforeRuleSection.add(tokenType!!)
                tokenType == PyTokenTypes.IDENTIFIER && tokensBeforeRuleSection.all { it in PyTokenTypes.WHITESPACE } ->
                    tokensBeforeRuleSection.add(tokenType!!)
                else -> {
                    expectingRuleSectionOnSameLine = false
                    if (!atToken(PyTokenTypes.IDENTIFIER)) {
                        tokensBeforeRuleSection.clear()
                    }
                }
            }
        }

        // a colon or a whitespace and then a colon follows a section keyword
        // if anything else occurred (e.g. statement break, identifier),
        // we are already inside the section, and then statement breaks should not occur,
        // so this variable is set to false
        if ((ruleLikeSectionIndent > -1 || isInToplevelSectionWithoutSubsections) &&
            beforeFirstArgumentInSection &&
            tokenType !in PyTokenTypes.WHITESPACE_OR_LINEBREAK &&
            !atToken(commentTokenType) &&
            !atToken(PyTokenTypes.COLON)
        ) {
            beforeFirstArgumentInSection = false
        }

        if (topLevelSectionIndent == -1 && (tokenText in KEYWORD_LIKE_SECTION_NAME_2_TOKEN_TYPE)) {
            val possibleToplevelSectionKeyword = tokenText
            if (isToplevelKeywordSection()) {
                // if it's the first token in the file, it's 0, as it should be
                topLevelSectionIndent = myCurrentNewlineIndent
                ruleLikeSectionIndent = -1
                isInPythonSection = possibleToplevelSectionKeyword in PYTHON_BLOCK_KEYWORDS
                expectingRuleSectionOnSameLine = !isInToplevelSectionWithoutSubsections
            }
        } else if (topLevelSectionIndent > -1 &&
            myCurrentNewlineIndent >= topLevelSectionIndent &&
            ruleLikeSectionIndent == -1 &&
            atToken(PyTokenTypes.IDENTIFIER) &&
            !isInToplevelSectionWithoutSubsections
        ) {
            val tryToIdentifyRuleSection =
                if (tokensBeforeRuleSection.isNotEmpty()) {
                    tokensBeforeRuleSection.contains(PyTokenTypes.COLON)
                } else {
                    tokenStart == myCurrentNewlineOffset
                }
            if (tryToIdentifyRuleSection) {
                val identifierPosition = currentPosition
                val identifierText = tokenText
                // look ahead and update rule-like section indent if an identifier is followed by a colon
                // this allows to avoid hardcoding section names and ensures correct lexing/parsing
                // of new snakemake sections should they be introduced in future releases
                advanceBase()
                if (atBaseToken(PyTokenTypes.COLON)) {
                    ruleLikeSectionIndent = myCurrentNewlineIndent
                    isInPythonSection = identifierText == SnakemakeNames.SECTION_RUN
                    beforeFirstArgumentInSection = true
                }
                restore(identifierPosition)
                tokensBeforeRuleSection.clear()
            }
        }

        if (atToken(PyTokenTypes.LINE_BREAK)) {
            /** IDEA uses a [com.intellij.openapi.editor.Document] object
             * which replaces all line separators with '\n'.
             * Even if the original file had CRLF for line separator,
             * tokenText will never contain CR, only LF.
             * Thus only '\n' can be used as a delimiter here, but [System.lineSeparator] cannot.
             */
            val text = tokenText.substringAfterLast('\n')
            var spaces = 0
            for (i in text.length - 1 downTo 0) {
                if (text[i] == ' ') {
                    spaces++
                } else if (text[i] == '\t') {
                    // Size 8 is used in com.jetbrains.python.lexer.PythonIndentingProcessor.advance()
                    // AFAIU is default continuation indent
                    spaces += 8
                }
            }
            myCurrentNewlineIndent = spaces
            myCurrentNewlineOffset = tokenEnd
            if (insideSnakemakeArgumentList(myCurrentNewlineIndent)
                { currentIndent, sectionIndent -> currentIndent <= sectionIndent }
            ) {
                val currentLineBreakIndex = myTokenQueue.indexOfFirst {
                    it.type === PyTokenTypes.LINE_BREAK && it.start == tokenStart
                }

                val isAtComment = if (currentLineBreakIndex != -1) {
                    var i = currentLineBreakIndex + 1
                    while (i < myTokenQueue.size && myTokenQueue[i].type in PyTokenTypes.WHITESPACE_OR_LINEBREAK) {
                        i++
                    }
                    i < myTokenQueue.size && myTokenQueue[i].type === commentTokenType || atBaseToken(commentTokenType)
                } else {
                    atBaseToken(commentTokenType)
                }

                if (!isAtComment) {
                    ruleLikeSectionIndent = -1
                    isInPythonSection = false
                }
            }
            if (ruleLikeSectionIndent == -1 && myCurrentNewlineIndent <= topLevelSectionIndent) {
                topLevelSectionIndent = -1
                isInToplevelSectionWithoutSubsections = false
                isInPythonSection = false
            }
        } else if (atToken(PyTokenTypes.TAB)) {
            // Size 8 is used in com.jetbrains.python.lexer.PythonIndentingProcessor.advance()
            // AFAIU is default continuation indent
            myCurrentNewlineIndent += 8
        }

        previousToken = tokenType

        if (myTokenQueue.size > 0) {
            myTokenQueue.removeAt(0)
            if (myProcessSpecialTokensPending) {
                myProcessSpecialTokensPending = false
                processSpecialTokens()
            }
        } else {
            advanceBase()
            processSpecialTokens()
        }
        adjustBraceLevel()
        currentToken = tokenType
    }

    private fun adjustBraceLevel() {
        val tokenType = tokenType

        when {
            tokenType in PyTokenTypes.OPEN_BRACES -> myBraceLevel++
            tokenType in PyTokenTypes.CLOSE_BRACES -> myBraceLevel--
            myBraceLevel != 0 -> {
                val recoveryTokens = PythonDialectsTokenSetProvider.getInstance().unbalancedBracesRecoveryTokens

                val leftPreviousSection = myCurrentNewlineIndent <= ruleLikeSectionIndent ||
                        ruleLikeSectionIndent == -1 && myCurrentNewlineIndent <= topLevelSectionIndent
                val isInPythonCode = isInPythonSection || topLevelSectionIndent == -1
                val isToplevelSectionKeyword = (leftPreviousSection && !isInPythonSection || isInPythonCode) &&
                        isToplevelKeywordSection()
                if (tokenType !in recoveryTokens && !isToplevelSectionKeyword) {
                    return
                }

                myBraceLevel = 0
                val pos = tokenStart
                pushToken(PyTokenTypes.STATEMENT_BREAK, pos, pos)
                val indents = myIndentStack.size()
                for (i in 0 until indents - 1) {
                    val indent = myIndentStack.peek()
                    if (myCurrentNewlineIndent >= indent) {
                        break
                    }
                    if (myIndentStack.size() > 1) {
                        myIndentStack.pop()
                        pushToken(PyTokenTypes.DEDENT, pos, pos)
                    }
                }
                pushToken(PyTokenTypes.LINE_BREAK, pos, pos)
            }
        }
    }

    private fun isToplevelKeywordSection(): Boolean {
        val possibleKeywordPosition = currentPosition
        val possibleToplevelSectionKeyword = tokenText

        if (possibleToplevelSectionKeyword !in KEYWORD_LIKE_SECTION_NAME_2_TOKEN_TYPE
            || tokenStart != myCurrentNewlineOffset
        ) {
            return false
        }

        advanceBase()
        // is currently the last word in the file or is followed by a colon or a whitespace, an identifier and a colon
        var isToplevelSection = (tokenType == PyTokenTypes.COLON || tokenType == null) &&
                (previousToken == null || previousToken == PyTokenTypes.LINE_BREAK || previousToken == PyTokenTypes.INDENT)

        if (isToplevelSection && possibleToplevelSectionKeyword !in RULE_LIKE_KEYWORDS) {
            restore(possibleKeywordPosition)
            isInToplevelSectionWithoutSubsections = true
            beforeFirstArgumentInSection = true
            return true
        }

        if (!isToplevelSection) {
            if (possibleToplevelSectionKeyword in RULE_LIKE_KEYWORDS) {
                while (tokenType in PyTokenTypes.WHITESPACE) {
                    advanceBase()
                }
                if (tokenType == PyTokenTypes.IDENTIFIER) {
                    advanceBase()
                    while (tokenType in PyTokenTypes.WHITESPACE) {
                        advanceBase()
                    }
                    if (tokenType == PyTokenTypes.COLON) {
                        isToplevelSection = true
                    }
                }
            }
        }
        restore(possibleKeywordPosition)

        return isToplevelSection
    }

    // TODO: it seems restore not always work ok, may be also restore some part of our complicated state?
    //override fun restore(position: LexerPosition) {
    //    super.restore(position)
    //}

    override fun processLineBreak(startPos: Int) {
        if ((ruleLikeSectionIndent > -1 || isInToplevelSectionWithoutSubsections)
            && !isInPythonSection && !beforeFirstArgumentInSection
        ) {
            if (myBraceLevel != 0) {
                processInsignificantLineBreak(startPos, false)
                return
            }

            val indentPos = currentPosition
            val hasSignificantTokens = myLineHasSignificantTokens
            val indent = nextLineIndent
            val isAtComment = atBaseToken(commentTokenType)
            restore(indentPos)
            if (isAtComment) {
                processComments(startPos)
                return
            }
            myLineHasSignificantTokens = hasSignificantTokens
            if (insideSnakemakeArgumentList(indent) { currentIndent, sectionIndent -> currentIndent > sectionIndent }) {
                processInsignificantLineBreak(startPos, false)
                processIndentsInsideSection(indent, startPos)
            } else {
                // section exit
                popIndentStackWhilePossible()
                super.processLineBreak(startPos)
            }
        } else {
            // inside python section/not inside snakemake section
            super.processLineBreak(startPos)
        }
    }

    /** Adds dedents where necessary. A simplified version of [closeDanglingSuitesWithComments] */
    private fun closeDanglingSuites(indent: Int, whiteSpaceStart: Int) {
        var lastIndent = myIndentStack.peek()

        var insertIndex = myTokenQueue.size
        while (indent < lastIndent) {
            myIndentStack.pop()
            lastIndent = myIndentStack.peek()
            myTokenQueue.add(insertIndex, PendingToken(PyTokenTypes.DEDENT, whiteSpaceStart, whiteSpaceStart))
            ++insertIndex
        }
    }

    private fun processIndentsInsideSection(indent: Int, startPos: Int) {
        val whiteSpaceEnd = if (baseTokenType == null) super.getBufferEnd() else baseTokenStart
        if (insideSnakemakeArgumentList(indent) { currentIndent, sectionIndent -> currentIndent < sectionIndent }) {
            closeDanglingSuites(indent, startPos)
            myTokenQueue.add(PendingToken(PyTokenTypes.LINE_BREAK, startPos, whiteSpaceEnd))
        } else if (indent < myIndentStack.peek()) {
            var lastIndent = myIndentStack.peek()

            // handle incorrect unindents if necessary
            while (indent < lastIndent) {
                myIndentStack.pop()
                if (insertedIndentsCount > 0) insertedIndentsCount--
                lastIndent = myIndentStack.peek()
                if (indent > lastIndent) {
                    myTokenQueue.add(PendingToken(PyTokenTypes.INCONSISTENT_DEDENT, startPos, startPos))
                    if (lastIndent <= ruleLikeSectionIndent ||
                        isInToplevelSectionWithoutSubsections && lastIndent <= topLevelSectionIndent
                    ) {
                        myIndentStack.push(indent)
                    }
                }
            }
            // to avoid breaking the token sequence
            if (myTokenQueue.find { it.start >= startPos } == null) {
                myTokenQueue.add(PendingToken(PyTokenTypes.LINE_BREAK, startPos, whiteSpaceEnd))
            }
        } else if (indent > myIndentStack.peek()) {
            myIndentStack.push(indent)
            insertedIndentsCount++
        }
    }

    // consumes comments and line breaks and processes the next line depending on the current section
    private fun processComments(startPos: Int) {
        var currentLineBreakStart = startPos
        while (atBaseToken(PyTokenTypes.LINE_BREAK)) {
            val linebreakPosition = currentPosition
            val hasSignificantTokens = myLineHasSignificantTokens
            val indent = nextLineIndent
            myLineHasSignificantTokens = hasSignificantTokens
            restore(linebreakPosition)
            processInsignificantLineBreak(currentLineBreakStart, false)
            if (atBaseToken(commentTokenType)) {
                if (linebreakBeforeFirstComment == -1) {
                    linebreakBeforeFirstComment = myTokenQueue.size - 1
                }
                myTokenQueue.add(PendingCommentToken(commentTokenType, baseTokenStart, baseTokenEnd, indent))
                advanceBase()
            } else {
                restore(linebreakPosition)
                break
            }
            currentLineBreakStart = baseTokenStart
        }

        val position = currentPosition
        val indent = nextLineIndent

        if (baseTokenType == null) {
            return
        }

        // insert statement break on section exit
        if (insideSnakemakeArgumentList(indent) { currentIndent, sectionIndent -> currentIndent <= sectionIndent }) {
            restore(position)
            val firstCommentQueueIndex = myTokenQueue.indexOfFirst { it.type == commentTokenType }
            val precedingToken = myTokenQueue[firstCommentQueueIndex - 1]
            if (precedingToken.type == PyTokenTypes.LINE_BREAK) {
                myTokenQueue.add(
                    firstCommentQueueIndex - 1,
                    PendingToken(
                        PyTokenTypes.STATEMENT_BREAK,
                        precedingToken.start,
                        precedingToken.start
                    )
                )
                if (linebreakBeforeFirstComment != -1) {
                    linebreakBeforeFirstComment++
                }

            }
            myLineHasSignificantTokens = false
            if (insideSnakemakeArgumentList(indent) { currentIndent, sectionIndent -> currentIndent < sectionIndent }) {
                popIndentStackWhilePossible()
                super.processIndent(myTokenQueue.last().end, PyTokenTypes.LINE_BREAK)
            }
        } else {
            processIndentsInsideSection(indent, baseTokenStart)
        }

        linebreakBeforeFirstComment = -1
    }

    override fun skipPrecedingCommentsWithSameIndentOnSuiteClose(indent: Int, anchorIndex: Int): Int {
        val anchor = if (linebreakBeforeFirstComment != -1) linebreakBeforeFirstComment else anchorIndex
        var result = anchor

        for (i in anchor until myTokenQueue.size) {
            val token = myTokenQueue[i] as PendingToken
            if (token is PendingCommentToken) {
                if (token.indent < indent) {
                    break
                }

                result = i + 1
            }
        }

        return result
    }

    private fun insideSnakemakeArgumentList(indent: Int, comparator: (Int, Int) -> Boolean) =
        ruleLikeSectionIndent > -1 && comparator(indent, ruleLikeSectionIndent) ||
                isInToplevelSectionWithoutSubsections && comparator(indent, topLevelSectionIndent)

    private fun popIndentStackWhilePossible() {
        while (insertedIndentsCount > 0) {
            if (myIndentStack.size() == 1) { // don't pop the very first indent
                insertedIndentsCount = 0
            } else {
                myIndentStack.pop()
                insertedIndentsCount--
            }
        }
    }

    private fun atToken(token: IElementType) = tokenType === token
    private fun atBaseToken(token: IElementType) = baseTokenType === token

    private class PendingCommentToken(
        type: IElementType,
        start: Int,
        end: Int,
        val indent: Int
    ) : PendingToken(type, start, end)
}