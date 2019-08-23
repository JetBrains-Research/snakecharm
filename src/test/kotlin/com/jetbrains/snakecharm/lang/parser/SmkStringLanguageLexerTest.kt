package com.jetbrains.snakecharm.lang.parser

import com.jetbrains.snakecharm.stringLanguage.lang.parser.SmkSLLexerAdapter

class SmkStringLanguageLexerTest : PyLexerTestCase() {
    fun testOrdinaryString() {
        doTest("just ordinary string with escaped {{ brackets }}",
                "STRING_CONTENT")
    }

    fun testLanguageWithTextAndDots() {
        doTest("some text {params.p1} text",
                "STRING_CONTENT", "LBRACE", "Py:IDENTIFIER",
                "DOT", "Py:IDENTIFIER", "RBRACE", "STRING_CONTENT")
    }

    fun testLanguageWithMultipleAccess() {
        doTest("{params.foo[key].boo[0][1]}",
                "LBRACE", "Py:IDENTIFIER", "DOT", "Py:IDENTIFIER",
                "LBRACKET", "Py:IDENTIFIER", "RBRACKET", "DOT",
                "Py:IDENTIFIER", "LBRACKET", "Py:IDENTIFIER", "RBRACKET",
                "LBRACKET", "Py:IDENTIFIER", "RBRACKET", "RBRACE")
    }

    fun testLanguageWithUnexpectedTokens() {
        doTest("{.[foo[key]..}",
                "LBRACE", "UNEXPECTED_TOKEN", "UNEXPECTED_TOKEN",
                "Py:IDENTIFIER", "LBRACKET", "Py:IDENTIFIER", "RBRACKET", "DOT",
                "UNEXPECTED_TOKEN", "RBRACE")
    }

    fun testLanguageWithRegexp() {
        doTest("{foo,\\d+}",
                "LBRACE", "Py:IDENTIFIER", "COMMA",
                "REGEXP", "RBRACE")
    }

    fun testMultipleLanguageInjectionWithRegexp() {
        doTest("{dataset,\\d+} text {input}",
                "LBRACE", "Py:IDENTIFIER", "COMMA",
                "REGEXP", "RBRACE", "STRING_CONTENT", "LBRACE",
                "Py:IDENTIFIER", "RBRACE")
    }

    fun testRegexpWithBraces() {
        doTest("{dataset,a{3,5}}",
                "LBRACE", "Py:IDENTIFIER", "COMMA",
                "REGEXP", "RBRACE")
    }

    fun testBracesOnly() {
        doTest("{}",
                "LBRACE", "RBRACE")
    }

    fun testIncompleteRegexp() {
        doTest("{foo, \\d+",
                "LBRACE", "Py:IDENTIFIER", "COMMA", "REGEXP")
    }


    fun testBracesAndComma() {
        doTest("{,}",
                "LBRACE", "COMMA", "RBRACE")
    }

    fun testCorrectIdentifierName() {
        doTest("{_correct_identifier_name10}",
                "LBRACE", "Py:IDENTIFIER", "RBRACE")
    }

    fun testBadIdentifierName() {
        doTest("{f*o&o?}",
                "LBRACE", "Py:IDENTIFIER", "UNEXPECTED_TOKEN",
                "Py:IDENTIFIER", "UNEXPECTED_TOKEN", "Py:IDENTIFIER",
                "UNEXPECTED_TOKEN", "RBRACE")
    }

    fun testRbracketInName() {
        doTest("{foo]}",
                "LBRACE", "Py:IDENTIFIER",
                "UNEXPECTED_TOKEN", "RBRACE")
    }

    fun testEscapedBracket() {
        doTest("{foo,\\}+}",
                "LBRACE", "Py:IDENTIFIER",
                "COMMA", "REGEXP", "RBRACE", "STRING_CONTENT")
    }

    private fun doTest(text: String, vararg expectedTokens: String) {
        doLexerTest(text, SmkSLLexerAdapter(), *expectedTokens)
    }
}
