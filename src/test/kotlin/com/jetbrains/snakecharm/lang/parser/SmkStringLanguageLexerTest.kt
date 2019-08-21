package com.jetbrains.snakecharm.lang.parser

import com.jetbrains.snakecharm.string_language.lang.parser.SmkSLLexerAdapter

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
                "LBRACKET", "ACCESS_KEY", "RBRACKET", "DOT",
                "Py:IDENTIFIER", "LBRACKET", "ACCESS_KEY", "RBRACKET",
                "LBRACKET", "ACCESS_KEY", "RBRACKET", "RBRACE")
    }

    fun testLanguageWithUnexpectedTokens() {
        doTest("{.[foo[key]..}",
                "LBRACE", "UNEXPECTED_TOKEN", "UNEXPECTED_TOKEN",
                "Py:IDENTIFIER", "LBRACKET", "ACCESS_KEY", "RBRACKET", "DOT",
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

    fun testWildcardWithSpaces() {
        doTest("{       sample       }",
                "LBRACE", "Py:IDENTIFIER", "RBRACE")
    }

    fun testFormatSpecifier() {
        doTest("{foo:%Y-%m-%d %H:%M:%S}",
                "LBRACE", "Py:IDENTIFIER", "FORMAT_SPECIFIER", "RBRACE")
    }

    fun testFormatSpecifierAndRegexp() {
        doTest("{foo:%Y-%m-%d %H:%M:%S,.+}",
                "LBRACE", "Py:IDENTIFIER", "FORMAT_SPECIFIER", "RBRACE")
    }

    fun testIdentifierNameWithSpacesBefore() {
        doTest("{   foo}",
                "LBRACE", "Py:IDENTIFIER", "RBRACE")
    }


    fun testIdentifierNameWithSpacesAfter() {
        doTest("{foo   }",
                "LBRACE", "Py:IDENTIFIER", "RBRACE")
    }

    fun testIdentifierNameWithSpaces() {
        doTest("{   foo   }",
                "LBRACE", "Py:IDENTIFIER", "RBRACE")
    }

    fun testIdentifierNameOnlySpaces() {
        doTest("{      }",
                "LBRACE", "Py:IDENTIFIER", "RBRACE")
    }

    private fun doTest(text: String, vararg expectedTokens: String) {
        doLexerTest(text, SmkSLLexerAdapter(), *expectedTokens)
    }
}