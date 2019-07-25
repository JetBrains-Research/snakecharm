package com.jetbrains.snakecharm.lang.parser

import com.google.common.collect.ImmutableMap
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

    companion object {
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

    //override fun getTokenType() = KEYWORDS[tokenText] ?: super.getTokenType()

    override fun advance() {
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
    }

    private fun adjustBraceLevel() {
        val tokenType = tokenType
        if (PyTokenTypes.OPEN_BRACES.contains(tokenType)) {
            myBraceLevel++
        } else if (PyTokenTypes.CLOSE_BRACES.contains(tokenType)) {
            myBraceLevel--
        } else if (myBraceLevel != 0 && (recoveryTokens.contains(tokenType) ||
                        (KEYWORDS[tokenText]!= null && myCurrentNewlineIndent < myIndentStack.peek()))) {
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