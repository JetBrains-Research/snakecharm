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
    private var previousToken: IElementType? = null
    // used to differentiate between 'rule all: input: "text"' and 'rule: input: "text" '
    private var topLevelSectionColonOccurred = false
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
            }
            restore(identifierPosition)
        }

        if (tokenType == PyTokenTypes.COLON && topLevelSectionIndent > -1 && ruleLikeSectionIndent == -1) {
            topLevelSectionColonOccurred = true
        }

        if (tokenType === PyTokenTypes.LINE_BREAK) {
            val text = tokenText
            var spaces = 0
            for (i in text.length - 1 downTo 0) {
                if (text[i] == ' ') {
                    spaces++
                } else if (text[i] == '\t') {
                    spaces += 8
                }
            }
            myCurrentNewlineIndent = spaces
            if (myCurrentNewlineIndent < ruleLikeSectionIndent || myCurrentNewlineIndent < topLevelSectionIndent) {
                // we left the previous section and didn't end up in a new one
                topLevelSectionIndent = -1
                ruleLikeSectionIndent = -1
                topLevelSectionColonOccurred = false
            }
        } else if (tokenType === PyTokenTypes.TAB) {
            myCurrentNewlineIndent += 8
        }

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
        previousToken = tokenType
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

        advanceBase()
        // is currently the last word in the file or is followed by a colon or a whitespace, an identifier and a colon
        var isToplevelSection = (tokenType == PyTokenTypes.COLON || tokenType == null) &&
                (previousToken == null || previousToken == PyTokenTypes.LINE_BREAK)
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
}