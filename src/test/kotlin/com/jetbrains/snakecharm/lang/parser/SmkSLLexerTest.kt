package com.jetbrains.snakecharm.lang.parser

import com.jetbrains.snakecharm.stringLanguage.lang.parser.SmkSLLexerAdapter

class SmkSLLexerTest : PyLexerTestCase() {
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
                "LBRACE", "BAD_CHARACTER", "BAD_CHARACTER",
                "Py:IDENTIFIER", "LBRACKET", "ACCESS_KEY", "RBRACKET", "DOT",
                "BAD_CHARACTER", "RBRACE")
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
                "LBRACE", "Py:IDENTIFIER", "BAD_CHARACTER",
                "Py:IDENTIFIER", "BAD_CHARACTER", "Py:IDENTIFIER",
                "BAD_CHARACTER", "RBRACE")
    }

    fun testRbracketInName() {
        doTest("{foo]}",
                "LBRACE", "Py:IDENTIFIER",
                "BAD_CHARACTER", "RBRACE")
    }

    fun testEscapedBracket() {
        doTest("{foo,\\}+}",
                "LBRACE", "Py:IDENTIFIER",
                "COMMA", "REGEXP", "RBRACE", "STRING_CONTENT")
    }

    fun testWildcardWithSpaces() {
        doTest("{       sample       }",
                "LBRACE", "BAD_CHARACTER", "Py:IDENTIFIER", "BAD_CHARACTER", "RBRACE")
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
                "LBRACE", "BAD_CHARACTER", "Py:IDENTIFIER", "RBRACE")
    }


    fun testIdentifierNameWithSpacesAfter() {
        doTest("{foo   }",
                "LBRACE", "Py:IDENTIFIER", "BAD_CHARACTER", "RBRACE")
    }

    fun testIdentifierNameWithSpaces() {
        doTest("{   foo   }",
                "LBRACE", "BAD_CHARACTER", "Py:IDENTIFIER", "BAD_CHARACTER", "RBRACE")
    }

    fun testIdentifierNameOnlySpaces() {
        doTest("{      }",
                "LBRACE", "BAD_CHARACTER", "RBRACE")
    }

    fun testDoubleDot() {
        doTest("{FOO..}",
                "LBRACE", "Py:IDENTIFIER", "DOT", "BAD_CHARACTER", "RBRACE")
    }

    fun testWhiteSpaceInIdentifier() {
        doTest("{foo boo[doo roo]}",
                "LBRACE", "Py:IDENTIFIER", "BAD_CHARACTER", "BAD_CHARACTER", "LBRACKET", "ACCESS_KEY", "RBRACKET", "RBRACE")
    }

    private fun doTest(text: String, vararg expectedTokens: String) {
        doLexerTest(text, SmkSLLexerAdapter(), *expectedTokens)
    }
}
