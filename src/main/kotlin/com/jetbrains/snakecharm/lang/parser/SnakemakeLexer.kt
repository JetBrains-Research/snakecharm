package com.jetbrains.snakecharm.lang.parser

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.intellij.psi.tree.IElementType
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.lexer.PythonIndentingLexer
import com.jetbrains.python.psi.PyElementType
import com.jetbrains.snakecharm.lang.SnakemakeNames

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeLexer : PythonIndentingLexer() {
    private val recoveryTokens = PythonDialectsTokenSetProvider.INSTANCE.unbalancedBracesRecoveryTokens
    private var myCurrentNewlineIndent = 0
    private var currentToken: IElementType? = null
    private var previousToken: IElementType? = null
    private var insertedIndentsCount = 0
    private var lineCommentInSectionEncountered = false
    private var addedStatementBreakAtEOF = false

    // used to differentiate between 'rule all: input: "text"' and 'rule: input: "text" '
    private var topLevelSectionColonOccurred = false

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
                .build()!!

        val PYTHON_BLOCK_KEYWORDS = ImmutableSet.Builder<String>()
                .add(SnakemakeNames.WORKFLOW_ONSTART_KEYWORD)
                .add(SnakemakeNames.WORKFLOW_ONSUCCESS_KEYWORD)
                .add(SnakemakeNames.WORKFLOW_ONERROR_KEYWORD)
                .build()!!

        val KEYWORDS = ImmutableMap.Builder<String, PyElementType>()
                .put(SnakemakeNames.RULE_KEYWORD, SnakemakeTokenTypes.RULE_KEYWORD)
                .put(SnakemakeNames.CHECKPOINT_KEYWORD, SnakemakeTokenTypes.CHECKPOINT_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_CONFIGFILE_KEYWORD, SnakemakeTokenTypes.WORKFLOW_CONFIGFILE_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_REPORT_KEYWORD, SnakemakeTokenTypes.WORKFLOW_REPORT_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD, SnakemakeTokenTypes.WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_SINGULARITY_KEYWORD, SnakemakeTokenTypes.WORKFLOW_SINGULARITY_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_INCLUDE_KEYWORD, SnakemakeTokenTypes.WORKFLOW_INCLUDE_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_WORKDIR_KEYWORD, SnakemakeTokenTypes.WORKFLOW_WORKDIR_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_LOCALRULES_KEYWORD, SnakemakeTokenTypes.WORKFLOW_LOCALRULES_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_RULEORDER_KEYWORD, SnakemakeTokenTypes.WORKFLOW_RULEORDER_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_ONSUCCESS_KEYWORD, SnakemakeTokenTypes.WORKFLOW_ONSUCCESS_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_ONERROR_KEYWORD, SnakemakeTokenTypes.WORKFLOW_ONERROR_KEYWORD)
                .put(SnakemakeNames.WORKFLOW_ONSTART_KEYWORD, SnakemakeTokenTypes.WORKFLOW_ONSTART_KEYWORD)
                .put(SnakemakeNames.SUBWORKFLOW_KEYWORD, SnakemakeTokenTypes.SUBWORKFLOW_KEYWORD)
                .build()!!

        val KEYWORDS_2_TEXT = KEYWORDS.map { it.value to it.key }.toMap()
    }

    override fun advance() {
        // a colon or a whitespace and then a colon follows a section keyword
        // if anything else occurred (e.g. statement break, identifier),
        // we are already inside the section, and then statement breaks should not occur,
        // so this variable is set to false
        if ((ruleLikeSectionIndent > -1 || isInToplevelSectionWithoutSubsections) &&
                beforeFirstArgumentInSection &&
                tokenType !in PyTokenTypes.WHITESPACE && tokenType != PyTokenTypes.COLON) {
            beforeFirstArgumentInSection = false
        }

        if (topLevelSectionIndent == -1 && KEYWORDS[tokenText] != null) {
            val possibleToplevelSectionKeyword = tokenText
            val isToplevelSection = isToplevelKeywordSection()
            if (isToplevelSection) {
                // if it's the first token in the file, it's 0, as it should be
                topLevelSectionIndent = myCurrentNewlineIndent
                ruleLikeSectionIndent = -1
                isInPythonSection = possibleToplevelSectionKeyword in PYTHON_BLOCK_KEYWORDS
            }
        } else if (topLevelSectionIndent > -1 &&
                myCurrentNewlineIndent >= topLevelSectionIndent &&
                ruleLikeSectionIndent == -1 &&
                tokenType == PyTokenTypes.IDENTIFIER && topLevelSectionColonOccurred) {
            val identifierPosition = currentPosition
            val identifierText = tokenText
            // look ahead and update rule-like section indent if an identifier is followed by a colon
            // this allows to avoid hardcoding section names and ensures correct lexing/parsing
            // of new snakemake sections should they be introduced in future releases
            advanceBase()
            if (baseTokenType == PyTokenTypes.COLON) {
                ruleLikeSectionIndent = myCurrentNewlineIndent
                isInPythonSection = identifierText == SnakemakeNames.SECTION_RUN
                beforeFirstArgumentInSection = true
            }
            restore(identifierPosition)
        }

        if (tokenType == PyTokenTypes.COLON && topLevelSectionIndent > -1 && ruleLikeSectionIndent == -1) {
            topLevelSectionColonOccurred = true
        }

        if (tokenType === PyTokenTypes.LINE_BREAK) {
            val text = tokenText.substringAfterLast(System.lineSeparator())
            var spaces = 0
            for (i in text.length - 1 downTo 0) {
                if (text[i] == ' ') {
                    spaces++
                } else if (text[i] == '\t') {
                    spaces += 8
                }
            }
            myCurrentNewlineIndent = spaces
            if (!isInToplevelSectionWithoutSubsections && topLevelSectionIndent > -1 &&
                    myCurrentNewlineIndent <= ruleLikeSectionIndent) {
                val currentLineBreakIndex = myTokenQueue.indexOfFirst {
                    it.type == PyTokenTypes.LINE_BREAK && it.start == tokenStart
                }
                val isAtComment = if (currentLineBreakIndex != -1) {
                    var i = currentLineBreakIndex + 1
                    while (i < myTokenQueue.size && myTokenQueue[i].type in PyTokenTypes.WHITESPACE_OR_LINEBREAK) {
                        i++
                    }
                    i < myTokenQueue.size && myTokenQueue[i].type == commentTokenType
                } else false
                if (!isAtComment) {
                    ruleLikeSectionIndent = -1
                    isInPythonSection = false
                }
            }
            if (ruleLikeSectionIndent == -1 && myCurrentNewlineIndent <= topLevelSectionIndent) {
                topLevelSectionIndent = -1
                topLevelSectionColonOccurred = false
                isInToplevelSectionWithoutSubsections = false
                isInPythonSection = false
            }
        } else if (tokenType === PyTokenTypes.TAB) {
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
        if (PyTokenTypes.OPEN_BRACES.contains(tokenType)) {
            myBraceLevel++
        } else if (PyTokenTypes.CLOSE_BRACES.contains(tokenType)) {
            myBraceLevel--
        } else if (myBraceLevel != 0) {
            val leftPreviousSection = myCurrentNewlineIndent <= ruleLikeSectionIndent ||
                    ruleLikeSectionIndent == -1 && myCurrentNewlineIndent <= topLevelSectionIndent
            val isInPythonCode = isInPythonSection || topLevelSectionIndent == -1
            val isToplevelSectionKeyword = (leftPreviousSection && !isInPythonSection || isInPythonCode) &&
                    isToplevelKeywordSection()
            if (!recoveryTokens.contains(tokenType) && !isToplevelSectionKeyword) {
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

    private fun isToplevelKeywordSection(): Boolean {
        val possibleKeywordPosition = currentPosition
        val possibleToplevelSectionKeyword = tokenText

        if (KEYWORDS[possibleToplevelSectionKeyword] == null) {
            return false
        }

        advanceBase()
        // is currently the last word in the file or is followed by a colon or a whitespace, an identifier and a colon
        var isToplevelSection = (tokenType == PyTokenTypes.COLON || tokenType == null) &&
                (previousToken == null || previousToken == PyTokenTypes.LINE_BREAK)

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

    override fun processLineBreak(startPos: Int) {
        if ((ruleLikeSectionIndent > -1 || isInToplevelSectionWithoutSubsections)
                && !isInPythonSection && !beforeFirstArgumentInSection) {
            val indentPos = currentPosition
            val hasSignificantTokens = myLineHasSignificantTokens
            var indent = nextLineIndent
            if (baseTokenType == commentTokenType) {
                if (myCurrentNewlineIndent >= ruleLikeSectionIndent ||
                        isInToplevelSectionWithoutSubsections && myCurrentNewlineIndent >= topLevelSectionIndent) {
                    restore(indentPos)
                    lineCommentInSectionEncountered = true
                    processInsignificantLineBreak(startPos, false)
                    return
                }
                indent = myIndentStack.peek()
                lineCommentInSectionEncountered = true
            }
            restore(indentPos)
            myLineHasSignificantTokens = hasSignificantTokens
            if (indent > ruleLikeSectionIndent && ruleLikeSectionIndent > -1 ||
                    isInToplevelSectionWithoutSubsections && indent > topLevelSectionIndent) {
                processInsignificantLineBreak(startPos, false)

                val whiteSpaceEnd = if (baseTokenType == null) super.getBufferEnd() else baseTokenStart
                if (ruleLikeSectionIndent > -1 && indent < ruleLikeSectionIndent
                        || isInToplevelSectionWithoutSubsections && indent < topLevelSectionIndent) {
                    // report error
                    closeDanglingSuites(indent, startPos)
                    myTokenQueue.add(PendingToken(PyTokenTypes.LINE_BREAK, startPos, whiteSpaceEnd))
                } else if (indent < myIndentStack.peek()) {
                    var lastIndent = myIndentStack.peek()

                    var insertIndex = myTokenQueue.size
                    // handle incorrect unindents if necessary
                    while (indent < lastIndent) {
                        myIndentStack.pop()
                        insertedIndentsCount--
                        lastIndent = myIndentStack.peek()
                        if (indent > lastIndent) {
                            myTokenQueue.add(PendingToken(PyTokenTypes.INCONSISTENT_DEDENT, startPos, startPos))
                            insertIndex = myTokenQueue.size
                        }
                        ++insertIndex
                    }
                    myTokenQueue.add(PendingToken(PyTokenTypes.LINE_BREAK, startPos, whiteSpaceEnd))
                } else if (indent > myIndentStack.peek()) {
                    myIndentStack.push(indent)
                    insertedIndentsCount++
                }
            } else {
                while (insertedIndentsCount > 0) {
                    myIndentStack.pop()
                    insertedIndentsCount--
                }
                if (lineCommentInSectionEncountered) {
                    val firstLineCommentPosition = myTokenQueue.indexOfFirst { it.type == commentTokenType }
                    if (firstLineCommentPosition > 0) {
                        val firstLineCommentPrecedingToken = myTokenQueue[firstLineCommentPosition - 1]
                        myTokenQueue.add(
                                firstLineCommentPosition - 1,
                                PendingToken(
                                        PyTokenTypes.STATEMENT_BREAK,
                                        firstLineCommentPrecedingToken.start,
                                        firstLineCommentPrecedingToken.start
                                )
                        )
                        myLineHasSignificantTokens = false
                    }

                }
                super.processLineBreak(startPos)
            }
        } else {
            super.processLineBreak(startPos)
        }
    }

    private fun closeDanglingSuites(indent: Int, whiteSpaceStart: Int) {
        var lastIndent = myIndentStack.peek()

        var insertIndex = myTokenQueue.size
        while (indent < lastIndent) {
            val lastSuiteIndent = myIndentStack.pop()
            lastIndent = myIndentStack.peek()
            var dedentOffset = whiteSpaceStart
            insertIndex = if (indent > lastIndent) {
                myTokenQueue.add(PendingToken(PyTokenTypes.INCONSISTENT_DEDENT, whiteSpaceStart, whiteSpaceStart))
                myTokenQueue.size
            } else {
                skipPrecedingCommentsWithSameIndentOnSuiteClose(lastSuiteIndent, insertIndex)
            }

            if (insertIndex != myTokenQueue.size) {
                dedentOffset = (myTokenQueue[insertIndex] as PendingToken).start
            }

            myTokenQueue.add(insertIndex, PendingToken(PyTokenTypes.DEDENT, dedentOffset, dedentOffset))
            ++insertIndex
        }
    }

    override fun skipPrecedingCommentsWithSameIndentOnSuiteClose(indent: Int, anchorIndex: Int): Int {
        if (!(ruleLikeSectionIndent > -1 || isInToplevelSectionWithoutSubsections)) {
            return super.skipPrecedingCommentsWithSameIndentOnSuiteClose(indent, anchorIndex)
        }

        var result = anchorIndex
        for (i in anchorIndex until myTokenQueue.size) {
            if (myTokenQueue[i].type == commentTokenType) {
                result = i + 1
            }
        }
        return result
    }

    override fun processSpecialTokens() {
        if (!(ruleLikeSectionIndent > -1 || isInToplevelSectionWithoutSubsections || isInPythonSection)) {
            super.processSpecialTokens()
            return
        }

        super.processSpecialTokens()
        if (baseTokenType == null && !addedStatementBreakAtEOF) {
            val firstLineCommentPosition = myTokenQueue.indexOfFirst { it.type == commentTokenType }
            if (firstLineCommentPosition > 0) {
                val firstLineCommentPrecedingToken = myTokenQueue[firstLineCommentPosition - 1]
                myTokenQueue.add(
                        firstLineCommentPosition - 1,
                        PendingToken(
                                PyTokenTypes.STATEMENT_BREAK,
                                firstLineCommentPrecedingToken.start,
                                firstLineCommentPrecedingToken.start
                        )
                )
                addedStatementBreakAtEOF = true
                val lastCommentTokenIndex = myTokenQueue.indexOfLast { it.type == commentTokenType }
                val extraStatementBreakIndex = myTokenQueue.indexOfFirst {
                    myTokenQueue.indexOf(it) > lastCommentTokenIndex &&
                            it.type == PyTokenTypes.STATEMENT_BREAK
                }
                if (extraStatementBreakIndex > 0 && extraStatementBreakIndex <= myTokenQueue.lastIndex) {
                    myTokenQueue.removeAt(extraStatementBreakIndex)
                }
            }
        }
    }
}