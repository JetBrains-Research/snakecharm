package com.jetbrains.snakecharm.lang.parser

import com.jetbrains.python.PythonDialectsTokenSetContributor
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.snakecharm.lang.SnakemakeTokenSetContributor

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

    /* TODO #16
    fun testRuleParamStringLiteralWithLineBreak() {
        Assume.assumeFalse(
                "Feature Not Implemented Yet, see: See issue https://github.com/JetBrains-Research/snakecharm/issues/16",
                true
        )
        doTest("""
            |@workflow.input(
            |    "{dataset}/inputfile"
            |    "fff"
            |)
            |
            |rule samtools_sort:
            |    input:
            |        "foo"
            |        "boo"
            |""".trimMargin().trimStart(),
                "Py:AT")
    }
    */

    fun testToplevelKeywordsOnTopLevel() {
        doTest("""
            |wildcard_constraints:
            |    foo = ".*"
            |report: "foo.rst"
            |""".trimMargin().trimStart(),
                "Py:WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:INDENT", "Py:IDENTIFIER", "Py:SPACE", "Py:EQ", "Py:SPACE", "Py:SINGLE_QUOTED_STRING",
                "Py:STATEMENT_BREAK", "Py:DEDENT", "Py:LINE_BREAK",
                "Py:WORKFLOW_REPORT_KEYWORD", "Py:COLON", "Py:SPACE", "Py:SINGLE_QUOTED_STRING",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:STATEMENT_BREAK")
    }
    fun testToplevelKeywordsInRule() {
        doTest("""
            |rule all:
            |    wildcard_constraints:
            |       boo = ".*"
            |    input:
            |       report("foo")
            |    singularity:
            |       "docker://continuumio/miniconda3:4.4.10"
            |""".trimMargin().trimStart(),
                "Py:RULE_KEYWORD", "Py:SPACE", "Py:IDENTIFIER", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:INDENT", "Py:WORKFLOW_WILDCARD_CONSTRAINTS_KEYWORD", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:INDENT", "Py:IDENTIFIER", "Py:SPACE", "Py:EQ", "Py:SPACE", "Py:SINGLE_QUOTED_STRING",
                "Py:STATEMENT_BREAK", "Py:DEDENT", "Py:LINE_BREAK",
                "Py:IDENTIFIER", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:INDENT", "Py:WORKFLOW_REPORT_KEYWORD", "Py:LPAR", "Py:SINGLE_QUOTED_STRING", "Py:RPAR",
                "Py:STATEMENT_BREAK", "Py:DEDENT", "Py:LINE_BREAK",
                "Py:WORKFLOW_SINGULARITY_KEYWORD", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:INDENT", "Py:SINGLE_QUOTED_STRING",
                "Py:STATEMENT_BREAK", "Py:DEDENT", "Py:DEDENT", "Py:LINE_BREAK",
                "Py:STATEMENT_BREAK")
    }


    /*

     SnakemakeLexer.processLineBreak(startPos: Int) {
//        rule samtools_sort:
//            input:
//                "foo"
//                "boo"
        // how to process insignificant line break here? instead of signif?
        // not clear from lexer side...

        super.processLineBreak(startPos)
        //TODO: line break in rules args
        // processInsignificantLineBreak(startPos, false)
    }


   see: com.jetbrains.python.lexer.PythonIndentingProcessor.adjustBraceLevel

      private void adjustBraceLevel() {
    final IElementType tokenType = getTokenType();
    if (PyTokenTypes.OPEN_BRACES.contains(tokenType)) {
      myBraceLevel++;
    }
    else if (PyTokenTypes.CLOSE_BRACES.contains(tokenType)) {
      myBraceLevel--;
    }
    else if (myBraceLevel != 0 && RECOVERY_TOKENS.contains(tokenType)) {
      myBraceLevel = 0;

    ....

     */

    private fun doTest(text: String, vararg expectedTokens: String) {
        doLexerTest(text, SnakemakeLexer(), *expectedTokens)
    }
}