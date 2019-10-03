package com.jetbrains.snakecharm.lang.parser

import com.jetbrains.python.PythonDialectsTokenSetContributor
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.snakecharm.lang.SmkTokenSetContributor

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */

class SnakemakeLexerTest : PyLexerTestCase() {
    override fun setUp() {
        super.setUp()
        registerExtension(PythonDialectsTokenSetContributor.EP_NAME, SmkTokenSetContributor())
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
                "Py:IDENTIFIER", "Py:SPACE", "Py:IDENTIFIER", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:STATEMENT_BREAK")
    }

    fun testSubworkflow() {
        doTest("""
            |subworkflow otherworkflow:
            |""".trimMargin().trimStart(),
                "Py:IDENTIFIER", "Py:SPACE", "Py:IDENTIFIER", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:STATEMENT_BREAK")
    }

    fun testRuleWithParams() {
        doTest("""
            |rule all:
            |    input: 'foo'
            |""".trimMargin().trimStart(),
                "Py:IDENTIFIER", "Py:SPACE", "Py:IDENTIFIER", "Py:COLON",
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
                "Py:IDENTIFIER", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:INDENT", "Py:IDENTIFIER", "Py:SPACE", "Py:EQ", "Py:SPACE", "Py:SINGLE_QUOTED_STRING",
                "Py:STATEMENT_BREAK", "Py:DEDENT", "Py:LINE_BREAK",
                "Py:IDENTIFIER", "Py:COLON", "Py:SPACE", "Py:SINGLE_QUOTED_STRING",
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
                "Py:IDENTIFIER", "Py:SPACE", "Py:IDENTIFIER", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:INDENT", "Py:IDENTIFIER", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:INDENT", "Py:IDENTIFIER", "Py:SPACE", "Py:EQ", "Py:SPACE", "Py:SINGLE_QUOTED_STRING",
                "Py:STATEMENT_BREAK", "Py:DEDENT", "Py:LINE_BREAK",
                "Py:IDENTIFIER", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:INDENT", "Py:IDENTIFIER", "Py:LPAR", "Py:SINGLE_QUOTED_STRING", "Py:RPAR",
                "Py:STATEMENT_BREAK", "Py:DEDENT", "Py:LINE_BREAK",
                "Py:IDENTIFIER", "Py:COLON",
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

    fun testSeveralRuleWithParams() {
        doTest("""
            |rule all:
            |    input: 'foo'
            |rule last:
            |    output: 'boo'
            |""".trimMargin().trimStart(),
                "Py:IDENTIFIER", "Py:SPACE", "Py:IDENTIFIER", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:INDENT", "Py:IDENTIFIER", "Py:COLON", "Py:SPACE", "Py:SINGLE_QUOTED_STRING",
                "Py:STATEMENT_BREAK", "Py:DEDENT", "Py:LINE_BREAK",
                "Py:IDENTIFIER",  "Py:SPACE", "Py:IDENTIFIER", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:INDENT", "Py:IDENTIFIER", "Py:COLON", "Py:SPACE", "Py:SINGLE_QUOTED_STRING",
                "Py:STATEMENT_BREAK", "Py:DEDENT", "Py:LINE_BREAK",
                "Py:STATEMENT_BREAK")
    }

    fun testRuleIncomplete1() {
        doTest("""
            |rule all:
            |rule last:
            |    output: 'boo'
            |""".trimMargin().trimStart(),
                "Py:IDENTIFIER", "Py:SPACE", "Py:IDENTIFIER", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:IDENTIFIER",  "Py:SPACE", "Py:IDENTIFIER", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:INDENT", "Py:IDENTIFIER", "Py:COLON", "Py:SPACE", "Py:SINGLE_QUOTED_STRING",
                "Py:STATEMENT_BREAK", "Py:DEDENT", "Py:LINE_BREAK",
                "Py:STATEMENT_BREAK")
    }

    fun testRuleIncomplete2() {
        doTest("""
            |rule all:
            |    
            |rule last:
            |    output: 'boo'
            |""".trimMargin().trimStart(),
                "Py:IDENTIFIER", "Py:SPACE", "Py:IDENTIFIER", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:IDENTIFIER",  "Py:SPACE", "Py:IDENTIFIER", "Py:COLON",
                "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
                "Py:INDENT", "Py:IDENTIFIER", "Py:COLON", "Py:SPACE", "Py:SINGLE_QUOTED_STRING",
                "Py:STATEMENT_BREAK", "Py:DEDENT", "Py:LINE_BREAK",
                "Py:STATEMENT_BREAK")
    }

    fun testIssue190() {
        // https//github.com/JetBrains-Research/snakecharm/issues/190
        doTest("""
            |rule all:    
            |   params:
            |       extra="--buffer_size 20G"     
            |       # fooo
            |""".trimMargin().trimStart()
            ,
            "Py:IDENTIFIER", "Py:SPACE", "Py:IDENTIFIER", "Py:COLON", "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
            "Py:INDENT", "Py:IDENTIFIER", "Py:COLON", "Py:STATEMENT_BREAK", "Py:LINE_BREAK",
            "Py:INDENT", "Py:IDENTIFIER", "Py:EQ", "Py:SINGLE_QUOTED_STRING", "Py:LINE_BREAK", "Py:END_OF_LINE_COMMENT", "Py:LINE_BREAK", "Py:STATEMENT_BREAK"
        )
    }

    private fun doTest(text: String, vararg expectedTokens: String) {
        doLexerTest(text, SnakemakeLexer(), *expectedTokens)
    }
}