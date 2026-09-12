package com.jetbrains.snakecharm.lang.parser

import com.intellij.lexer.Lexer
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.BareTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import junit.framework.TestCase

/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 *
 * Historically this extended the platform's `PlatformLiteFixture` and manually registered the
 * `PythonDialectsTokenSetContributor` extension point on a mock application. That fixture was removed
 * in the 2026.1 (build 261) test framework, so we now stand up a real test *application* via
 * [BareTestFixture]: the Python plugin it loads already registers its token-set contributors, so the
 * snakemake lexer tokenizes exactly as it does at runtime.
 *
 * Deliberately **not** [com.intellij.testFramework.fixtures.BasePlatformTestCase]: [doLexerTest] never
 * touches a `CodeInsightTestFixture`, and lexing needs nothing project-scoped. Asking for a project
 * would build one light project per class off `LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR`,
 * which differs from the Python descriptors the Cucumber suite uses — the light-fixture framework
 * would then tear down and rebuild the shared project around these tests (see AGENTS.md → "Scenario
 * isolation is thinner than it looks").
 */
abstract class PyLexerTestCase : UsefulTestCase() {
    private val bareFixture: BareTestFixture = IdeaTestFixtureFactory.getFixtureFactory().createBareFixture()

    override fun setUp() {
        super.setUp()
        bareFixture.setUp()
    }

    override fun tearDown() {
        try {
            bareFixture.tearDown()
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
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