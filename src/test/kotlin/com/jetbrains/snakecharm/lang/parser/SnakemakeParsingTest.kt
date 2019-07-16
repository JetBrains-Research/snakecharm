package com.jetbrains.snakecharm.lang.parser

import com.intellij.lang.ASTFactory
import com.intellij.lang.LanguageASTFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ParsingTestCase
import com.jetbrains.python.*
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.impl.PythonASTFactory
import com.jetbrains.snakecharm.SnakemakeTestUtil
import com.jetbrains.snakecharm.lang.SmkTokenSetContributor


/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 *
 */
class SnakemakeParsingTest : ParsingTestCase(
        "psi", "smk", SnakemakeParserDefinition(), PythonParserDefinition()
) {
    private var myLanguageLevel = LanguageLevel.getDefault()

    override fun setUp() {
        super.setUp()
        registerExtensionPoint(PythonDialectsTokenSetContributor.EP_NAME, PythonDialectsTokenSetContributor::class.java)
        registerExtension(PythonDialectsTokenSetContributor.EP_NAME, PythonTokenSetContributor())
        registerExtension(PythonDialectsTokenSetContributor.EP_NAME, SmkTokenSetContributor())
        addExplicitExtension<ASTFactory>(LanguageASTFactory.INSTANCE, PythonLanguage.getInstance(), PythonASTFactory())
        PythonDialectsTokenSetProvider.reset()
    }

    override fun getTestDataPath() = SnakemakeTestUtil.getTestDataPath().toString()

    override fun createFile(name: String, text: String): PsiFile {
        val file = super.createFile(name, text)
        file.virtualFile.putUserData(LanguageLevel.KEY, myLanguageLevel)
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

    fun testRuleUnexpKeyword() {
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

    private fun doTest() {
        // Actually snakemake requires python 3.x and no need to have it working with python 2.x
        //doTest(LanguageLevel.fromPythonVersion("2"))

        doTest(LanguageLevel.fromPythonVersion("3"))
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