package com.jetbrains.snakemake.lang.parser

import com.jetbrains.python.PythonDialectsTokenSetContributor
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.snakemake.lang.SnakemakeTokenSetContributor

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeLexerTest : PyLexerTestCase() {
    override fun setUp() {
        super.setUp()
        registerExtension(PythonDialectsTokenSetContributor.EP_NAME, SnakemakeTokenSetContributor())
        PythonDialectsTokenSetProvider.reset()
    }

    fun testPythonExprAssignment() {
        doTest(
                "TRACK = 'hg19.gtf'\n",
                "Py:IDENTIFIER", "Py:SPACE", "Py:EQ", "Py:SPACE", "Py:SINGLE_QUOTED_STRING",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:STATEMENT_BREAK")
    }

    fun testRule() {
        doTest("""
            |rule all:
            |""".trimMargin().trimStart(),
                "Py:RULE_KEYWORD", "Py:SPACE", "Py:IDENTIFIER", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:STATEMENT_BREAK")
    }

    fun testRuleWithParams() {
        doTest("""
            |rule all:
            |    input: 'foo'
            |""".trimMargin().trimStart(),
                "Py:RULE_KEYWORD", "Py:SPACE", "Py:IDENTIFIER", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:INDENT", "Py:IDENTIFIER", "Py:COLON", "Py:SPACE", "Py:SINGLE_QUOTED_STRING",
                "Py:STATEMENT_BREAK", "Py:DEDENT", "Py:LINE_BREAK",
                "Py:STATEMENT_BREAK")
    }

    private fun doTest(text: String, vararg expectedTokens: String) {
        doLexerTest(text, SnakemakeLexer(), *expectedTokens)
    }
}