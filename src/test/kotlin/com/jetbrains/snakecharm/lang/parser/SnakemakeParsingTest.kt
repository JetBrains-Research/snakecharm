package com.jetbrains.snakecharm.lang.parser

import com.intellij.lang.LanguageASTFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ParsingTestCase
import com.jetbrains.python.PythonDialectsTokenSetContributor
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.PythonParserDefinition
import com.jetbrains.python.PythonTokenSetContributor
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyPsiFacade
import com.jetbrains.python.psi.PythonVisitorFilter
import com.jetbrains.python.psi.impl.PyPsiFacadeImpl
import com.jetbrains.python.psi.impl.PythonASTFactory
import com.jetbrains.python.psi.impl.PythonLanguageLevelPusher
import com.jetbrains.snakecharm.SnakemakeTestUtil
import com.jetbrains.snakecharm.lang.SmkTokenSetContributor
import com.jetbrains.snakecharm.lang.SnakemakeLanguageDialect


/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 *
 */
class SnakemakeParsingTest : ParsingTestCase(
    "psi", "smk", SmkParserDefinition(), PythonParserDefinition()
) {
    private var myLanguageLevel = LanguageLevel.getDefault()

    override fun setUp() {
        super.setUp()

        // Parsing tests doesn't use real intellij app => we cannot use
        // TestApplicationManager.getInstance() here

        // w/o this cannot instantiate SnakemakeLexer
        registerExtensionPoint(PythonDialectsTokenSetContributor.EP_NAME, PythonDialectsTokenSetContributor::class.java)
        registerExtension(PythonDialectsTokenSetContributor.EP_NAME, PythonTokenSetContributor())
        registerExtension(PythonDialectsTokenSetContributor.EP_NAME, SmkTokenSetContributor())
        addExplicitExtension(LanguageASTFactory.INSTANCE, PythonLanguage.getInstance(), PythonASTFactory())

        // w/o this fails due to NPEs on PyPsiFacade access
        project.registerService(
            PyPsiFacade::class.java,
            PyPsiFacadeImpl::class.java
        )
    }

    override fun tearDown() {
        // We have to force clean language extensions cache here, because these parser tests don't use real
        // test application and don't load all required extensions.
        // E.g. PythonId.visitorFilter EP will not load `SnakemakeVisitorFilter` and as a result other tests in test suite will fail
        val languageExtension = PythonVisitorFilter.INSTANCE
        languageExtension.clearCache(SnakemakeLanguageDialect)
        languageExtension.clearCache(PythonLanguage.INSTANCE)

        super.tearDown()
    }

    override fun getTestDataPath() = SnakemakeTestUtil.getTestDataPath().toString()

    override fun createFile(name: String, text: String): PsiFile {
        val file = super.createFile(name, text)
        PythonLanguageLevelPusher.specifyFileLanguageLevel(file.virtualFile, myLanguageLevel)
        return file
    }

    fun testPythonCode() {
        doTest()
    }

    fun testRule() {
        doTest()
    }

    fun testRuleInPythonBlock() {
        doTest()
    }

    fun testCheckpoint() {
        doTest()
    }

    fun testRuleNoName() {
        doTest()
    }

    fun testRuleMultiple() {
        doTest()
    }

    fun testRuleMultipleSingleLine() {
        doTest()
    }

    fun testRuleParams() {
        doTest()
    }

    fun testRuleInvalid() {
        doTest()
    }

    fun testRuleInvalidNoParamBody() {
        doTest()
    }

    fun testRuleInvalidNoParamBodyEof() {
        doTest()
    }

    fun testRuleInvalidParam() {
        doTest()
    }

    fun testRuleMultipleSingleLineNoBreak() {
        doTest()
    }

    fun testUnexpectedSectionKeyword() {
        doTest()
    }

    fun testRuleParamsListArgs() {
        doTest()
    }

    fun testRuleParamsListArgsKeywords() {
        doTest()
    }

    fun testRuleParamsListArgsStringMultiline() {
        doTest()
    }

    fun testRuleParamsListArgsHangingComma() {
        doTest()
    }

    fun testRuleParamsListArgsMultiple() {
        doTest()
    }

    fun testRuleParamsListArgsIndents() {
        doTest()
    }

    fun testRuleParamsListKeywordArgs() {
        doTest()
    }

    fun testRuleParamsListKeywordArgsMultiple() {
        doTest()
    }

    fun testRuleRun() {
        doTest()
    }

    fun testRuleRunPythonBlock() {
        doTest()
    }

    fun testRuleIncomplete1() {
        doTest()
    }

    fun testRuleIncomplete2() {
        doTest()
    }

    fun testRuleIncomplete3() {
        doTest()
    }

    fun testRuleIncomplete4() {
        doTest()
    }

    fun testRuleIncomplete5() {
        doTest()
    }

    fun testWorkflowParamsListArgsKeywords() {
        doTest()
    }

    fun testWorkflowParamsListArgsKeywordsInRule() {
        doTest()
    }

    fun testWorkflowTopLevelDecoratorsInRuleAsKeywordParams() {
        doTest()
    }

    fun testWorkflowPythonCodeBlockKeywords() {
        doTest()
    }

    fun testWorkflowRuleorder() {
        doTest()
    }

    fun testWorkflowRuleorderHangingSeparator() {
        doTest()
    }

    fun testWorkflowRuleorderInvalid() {
        doTest()
    }

    fun testWorkflowLocalrules() {
        doTest()
    }

    fun testWorkflowLocalrulesInvalid() {
        doTest()
    }

    fun testWorkflowLocalrulesHangingComma() {
        doTest()
    }

    fun testWorkflowUnknownSections() {
        doTest()
    }

    fun testSingleLineDocstring() {
        doTest()
    }

    fun testMultiLineDocstrings() {
        doTest()
    }

    fun testSingleQuotedDocstrings() {
        doTest()
    }

    fun testCrazyDocstrings() {
        doTest()
    }

    fun testHangingComa() {
        doTest()
    }

    fun testSingleSubworkflow() {
        doTest()
    }

    fun testRuleMultipleSingleLineWithRuleSectionIndent() {
        doTest()
    }

    fun testRuleParamsListArgsStringMultilineIncorrectUnindent() {
        doTest()
    }

    fun testFormattedStringArgument() {
        doTest()
    }

    fun testRuleMultilineStringArgumentsWithCallsAndExplicitConcatenation() {
        doTest()
    }

    fun testRuleStringCallExpressionArgument() {
        doTest()
    }

    fun testUnbalancedBracesRecovery() {
        doTest()
    }

    fun testKeywordLikeIdentifiersAsIdentifiers() {
        doTest()
    }

    fun testKeywordLikeIdentifiersAsKeywords() {
        doTest()
    }

    fun testIssue130() {
        doTest()
    }

    fun testIssue190() {
        doTest()
    }

    fun testDocstringAtEndOfFile() {
        doTest()
    }

    fun testKeywordIdentifiersAtToplevelWithPrecedingOpenBrace() {
        doTest()
    }

    fun testKeywordIdentifierWithNoIndent() {
        doTest()
    }

    fun testRuleParamsListArgsIncorrectUnindent() {
        doTest()
    }

    fun testRuleParamsListArgsIncorrectIndentation() {
        doTest()
    }

    fun testRuleParamsListArgsLineComments() {
        doTest()
    }

    fun testRuleParamsListArgsIndentationInsideBraces() {
        doTest()
    }

    fun testRuleParamsListArgsIncorrectRuleSectionLevelIndentation() {
        doTest()
    }

    fun testFormattedStringAfterToplevelSection() {
        doTest()
    }

    fun testIssue202() {
        doTest()
    }

    fun testWorkflowContainer() {
        doTest()
    }

    fun testWorkflowEnvvars() {
        doTest()
    }


    fun testFormattedStringInToplevelStatements() {
        doTest()
    }

    fun testRuleSectionNotebook() {
        doTest()
    }

    fun testRuleSectionContainer() {
        doTest()
    }

    fun testRuleSectionEnvmodules() {
        doTest()
    }

    fun testIssue275() {
        doTest()
    }

    fun testIssue275_1() {
        // minimal test case
        doTest()
    }

    fun testIssue275_2() {
        // minimal test case + workaround
        doTest()
    }

    fun testIssue313_1() {
        doTest()
    }

    fun testIssue313_2() {
        doTest()
    }

    fun testIssue175() {
        doTest()
    }

    fun testRuleSectionName() {
        // issue #351
        doTest()
    }

    private fun doTest() {
        // Actually snakemake requires python 3.x and no need to have it working with python 2.x
        //doTest(LanguageLevel.fromPythonVersion("2"))

        doTest(LanguageLevel.fromPythonVersion("3")!!)
    }

    private fun doTest(languageLevel: LanguageLevel) {
        val prev = myLanguageLevel
        myLanguageLevel = languageLevel
        try {
            doTest(true)
        } finally {
            myLanguageLevel = prev
        }
        // TODO: rule...
        //ensureEachFunctionHasStatementList(myFile, CythonFunction::class.java)
        ensureEachFunctionHasStatementList(myFile, PyFunction::class.java)
    }

    private fun <T : PyFunction> ensureEachFunctionHasStatementList(
        parentFile: PsiFile,
        functionType: Class<T>
    ) {
        val functions = PsiTreeUtil.findChildrenOfType(parentFile, functionType)
        for (functionToCheck in functions) {
            functionToCheck.statementList //To make sure each function has statement list (does not throw exception)
        }
    }

}