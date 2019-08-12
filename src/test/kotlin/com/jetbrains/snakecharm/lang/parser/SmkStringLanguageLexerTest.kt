package com.jetbrains.snakecharm.lang.parser

import com.jetbrains.snakecharm.string_language.lang.parser.SmkSLLexerAdapter

class SmkStringLanguageLexerTest : PyLexerTestCase() {
    fun testOrdinaryString() {
        doTest("just ordinary string with escaped {{ brackets }}",
                "STRING_CONTENT")
    }

    fun testLanguageWithTextAndDots() {
        doTest("some text {params.p1} text",
                "STRING_CONTENT", "LBRACE", "IDENTIFIER",
                "DOT", "IDENTIFIER", "RBRACE", "STRING_CONTENT")
    }

    fun testLanguageWithMultipleAccess() {
        doTest("{params.foo[key].boo[0][1]}",
                "LBRACE", "IDENTIFIER", "DOT", "IDENTIFIER",
                "LBRACKET", "ACCESS_KEY", "RBRACKET", "DOT",
                "IDENTIFIER", "LBRACKET", "ACCESS_KEY", "RBRACKET",
                "LBRACKET", "ACCESS_KEY", "RBRACKET", "RBRACE")
    }

    fun testLanguageWithUnexpectedTokens() {
        doTest("{.[foo[key]..}",
                "LBRACE", "UNEXPECTED_TOKEN", "UNEXPECTED_TOKEN",
                "IDENTIFIER", "LBRACKET", "ACCESS_KEY", "RBRACKET", "DOT",
                "UNEXPECTED_TOKEN", "RBRACE")
    }

    fun testLanguageWithRegexp() {
        doTest("{foo,\\d+}",
                "LBRACE", "IDENTIFIER", "COMMA",
                "REGEXP", "RBRACE")
    }

    fun testMultipleLanguageInjectionWithRegexp() {
        doTest("{dataset,\\d+} text {input}",
                "LBRACE", "IDENTIFIER", "COMMA",
                "REGEXP", "RBRACE", "STRING_CONTENT", "LBRACE",
                "IDENTIFIER", "RBRACE")
    }

    fun testRegexpWithBraces() {
        doTest("{dataset,a{3,5}}",
                "LBRACE", "IDENTIFIER", "COMMA",
                "REGEXP", "RBRACE")
    }

    fun testBracesOnly() {
        doTest("{}",
                "LBRACE", "RBRACE")
    }

    fun testIncompleteRegexp() {
        doTest("{foo, \\d+",
                "LBRACE", "IDENTIFIER", "COMMA", "REGEXP")
    }


    fun testBracesAndComma() {
        doTest("{,}",
                "LBRACE", "COMMA", "RBRACE")
    }

    fun testCorrectIdentifierName() {
        doTest("{_correct_identifier_name10}",
                "LBRACE", "IDENTIFIER", "RBRACE")
    }

    fun testBadIdentifierName() {
        doTest("{f*o&o?}",
                "LBRACE", "IDENTIFIER", "UNEXPECTED_TOKEN",
                "IDENTIFIER", "UNEXPECTED_TOKEN", "IDENTIFIER",
                "UNEXPECTED_TOKEN", "RBRACE")
    }

    fun testRbracketInName() {
        doTest("{foo]}",
                "LBRACE", "IDENTIFIER",
                "UNEXPECTED_TOKEN", "RBRACE")
    }

    private fun doTest(text: String, vararg expectedTokens: String) {
        doLexerTest(text, SmkSLLexerAdapter(), *expectedTokens)
    }
}