package com.jetbrains.snakecharm.lang.parser

import com.intellij.testFramework.ParsingTestCase
import com.jetbrains.python.PythonParserDefinition
import com.jetbrains.snakecharm.SnakemakeTestUtil
import com.jetbrains.snakecharm.stringLanguage.lang.parser.SmkSLParserDefinition
import org.intellij.lang.regexp.RegExpParserDefinition

class SmkSLParsingTest : ParsingTestCase(
    "stringLanguagePsi",
    "smkStringLanguage",
    SmkSLParserDefinition(),
    PythonParserDefinition(),
    RegExpParserDefinition()
) {

    override fun getTestDataPath(): String = SnakemakeTestUtil.getTestDataPath().toString()

    fun testMultipleAccess() {
        doTest(true)
    }

    fun testSubscription() {
        doTest(true)
    }

    fun testDotsInsteadOfIdentifiers() {
        doTest(true)
    }

    fun testNoIdentifier() {
        doTest(true)
    }

    fun testOrdinaryRegexp() {
        doTest(true)
    }

    fun testMultipleLanguageSections() {
        doTest(true)
    }

    fun testLbraceAndComma() {
        doTest(true)
    }

    fun testBracesAndComma() {
        doTest(true)
    }

    fun testLbraceIdentifier() {
        doTest(true)
    }

    fun testLbraceIdentifierDot() {
        doTest(true)
    }

    fun testRegexpOnly() {
        doTest(true)
    }

    fun testLbraceSubscription() {
        doTest(true)
    }

    fun testLbraceIncompleteSubscription1() {
        doTest(true)
    }

    fun testLbraceIncompleteSubscription2() {
        doTest(true)
    }

    fun testBadIdentifierName() {
        doTest(true)
    }

    fun testRbracketInName() {
        doTest(true)
    }

    fun testSpacesInIdentifierName() {
        doTest(true)
    }

    fun testFormatSpecifier() {
        doTest(true)
    }

    fun testInjectionWithSpaceInside() {
        doTest(true)
    }

    fun testMissingIdentifier() {
        doTest(true)
    }

    fun testMissingDot() {
        doTest(true)
    }
}