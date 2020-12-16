package com.jetbrains.snakecharm.lang.parser

import com.intellij.lexer.Lexer
import com.intellij.testFramework.PlatformLiteFixture
import com.jetbrains.python.PythonDialectsTokenSetContributor
import com.jetbrains.python.PythonTokenSetContributor
import junit.framework.TestCase

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 *
 * TODO: Ask for API
 */
abstract class PyLexerTestCase  : PlatformLiteFixture() {
    override fun setUp() {
        super.setUp()
        initApplication()
        registerExtensionPoint(PythonDialectsTokenSetContributor.EP_NAME, PythonDialectsTokenSetContributor::class.java)
        registerExtension(PythonDialectsTokenSetContributor.EP_NAME, PythonTokenSetContributor())
    }

    fun doLexerTest(text: String, lexer: Lexer, vararg expectedTokens: String) {
        doLexerTest(text, lexer, false, *expectedTokens)
    }

    fun doLexerTest(text: String,
                    lexer: Lexer,
                    checkTokenText: Boolean,
                    vararg expectedTokens: String) {
        lexer.start(text)
        var idx = 0
        var tokenPos = 0
        while (lexer.tokenType != null) {
            if (idx >= expectedTokens.size) {
                val remainingTokens = StringBuilder()
                while (lexer.tokenType != null) {
                    if (remainingTokens.isNotEmpty()) {
                        remainingTokens.append(", ")
                    }
                    remainingTokens.append("\"").append(if (checkTokenText) lexer.tokenText else lexer.tokenType!!.toString()).append("\"")
                    lexer.advance()
                }
                TestCase.fail("Too many tokens in file. Remaining unexpected tokens: $remainingTokens")
            }
            TestCase.assertEquals(
                "Token offset mismatch at lexeme $idx ${expectedTokens[idx]}, " +
                        "tokenText: <${lexer.tokenText}>; tokenType: ${lexer.tokenType};" +
                        " prev token end: ${tokenPos}; current token start: ${lexer.tokenStart}",
                tokenPos,  lexer.tokenStart
            )
            val tokenName = if (checkTokenText) lexer.tokenText else lexer.tokenType!!.toString()
            TestCase.assertEquals("Token mismatch at position $idx", expectedTokens[idx], tokenName)
            idx++
            tokenPos = lexer.tokenEnd
            lexer.advance()
        }

        if (idx < expectedTokens.size) TestCase.fail("Not enough tokens in file, expected: ${
            (idx until expectedTokens.size).joinToString { expectedTokens[it] }
        }")
    }
}