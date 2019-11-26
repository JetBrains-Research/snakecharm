package com.jetbrains.snakecharm.lang.parser

import com.intellij.lang.ASTFactory
import com.intellij.lang.LanguageASTFactory
import com.intellij.testFramework.ParsingTestCase
import com.jetbrains.python.*
import com.jetbrains.python.psi.PyPsiFacade
import com.jetbrains.python.psi.impl.PyPsiFacadeImpl
import com.jetbrains.python.psi.impl.PythonASTFactory
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

    override fun setUp() {
        super.setUp()
        registerExtensionPoint(PythonDialectsTokenSetContributor.EP_NAME, PythonDialectsTokenSetContributor::class.java)
        registerExtension(PythonDialectsTokenSetContributor.EP_NAME, PythonTokenSetContributor())
        addExplicitExtension<ASTFactory>(LanguageASTFactory.INSTANCE, PythonLanguage.getInstance(), PythonASTFactory())
        PythonDialectsTokenSetProvider.reset()
        project.registerService(
            PyPsiFacade::class.java,
            PyPsiFacadeImpl::class.java
        )
    }

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