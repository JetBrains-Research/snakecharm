package com.jetbrains.snakecharm.lang.parser

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ParsingTestCase
import com.jetbrains.python.PythonDialectsTokenSetContributor
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.PythonParserDefinition
import com.jetbrains.python.PythonTokenSetContributor
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.snakecharm.SnakemakeTestUtil
import com.jetbrains.snakecharm.lang.SnakemakeTokenSetContributor
import org.junit.*
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 *
 */
@RunWith(JUnit4::class)
class SnakemakeParsingTest : ParsingTestCase(
        "psi", "smk", SnakemakeParserDefinition(), PythonParserDefinition()
) {
    private var myLanguageLevel = LanguageLevel.getDefault()
    @Rule
    @JvmField
    var currentTestName = TestName()

    override fun getName() = currentTestName.methodName.capitalize()

    @Before
    public override fun setUp() {
        super.setUp()
        registerExtensionPoint(PythonDialectsTokenSetContributor.EP_NAME, PythonDialectsTokenSetContributor::class.java)
        registerExtension(PythonDialectsTokenSetContributor.EP_NAME, PythonTokenSetContributor())
        registerExtension(PythonDialectsTokenSetContributor.EP_NAME, SnakemakeTokenSetContributor())
        PythonDialectsTokenSetProvider.reset()
    }
    
    @After
    public override fun tearDown() {
        super.tearDown()
    }

    override fun getTestDataPath() = SnakemakeTestUtil.getTestDataPath().toString()

    override fun createFile(name: String, text: String): PsiFile {
        val file = super.createFile(name, text)
        file.virtualFile.putUserData(LanguageLevel.KEY, myLanguageLevel)
        return file
    }

    @Test 
    fun pythonCode() {
        doTest()
    }

    @Test 
    fun rule() {
        doTest()
    }

    @Test 
    fun ruleInPythonBlock() {
        doTest()
    }

    @Test 
    fun checkpoint() {
        doTest()
    }

    @Test 
    fun ruleNoName() {
        doTest()
    }

    @Test 
    fun ruleMultiple() {
        doTest()
    }

    @Test 
    fun ruleMultipleSingleLine() {
        doTest()
    }

    @Test 
    fun ruleParams() {
        doTest()
    }

    @Test 
    fun ruleInvalid() {
        doTest()
    }


    @Test 
    fun ruleInvalidNoParamBody() {
        doTest()
    }

    @Test 
    fun ruleInvalidNoParamBodyEof() {
        doTest()
    }

    @Test 
    fun ruleInvalidParam() {
        doTest()
    }

    @Test 
    fun ruleMultipleSingleLineNoBreak() {
        doTest()
    }

    @Test 
    fun ruleUnexpKeyword() {
        doTest()
    }

    @Test 
    fun ruleParamsListArgs() {
        doTest()
    }

    @Test 
    fun ruleParamsListArgsKeywords() {
        doTest()
    }

    @Ignore(value = "See issue https://github.com/JetBrains-Research/snakecharm/issues/16")
    @Test 
    fun ruleParamsListArgsStringMultiline() {
        doTest()
    }

    @Test 
    fun ruleParamsListArgsHangingComma() {
        doTest()
    }

    @Test 
    fun ruleParamsListArgsMultiple() {
        doTest()
    }

    @Test 
    fun ruleParamsListArgsIndents() {
        doTest()
    }

    @Test 
    fun ruleParamsListKeywordArgs() {
        doTest()
    }

    @Test 
    fun ruleParamsListKeywordArgsMultiple() {
        doTest()
    }

    @Test
    fun ruleRun() {
        doTest()
    }

    @Test 
    fun ruleRunPythonBlock() {
        doTest()
    }

    @Test
    fun workflowParamsListArgsKeywords() {
        doTest()
    }

    @Test
    fun workflowParamsListArgsKeywordsInRule() {
        doTest()
    }

    @Test
    fun workflowTopLevelDecoratorsInRuleAsKeywordParams() {
        doTest()
    }

    @Test
    fun workflowPythonCodeBlockKeywords() {
        doTest()
    }

    @Test
    @Ignore(value = "See https://github.com/JetBrains-Research/snakecharm/issues/30")
    fun workflowRuleReorder() {
        doTest()
    }

    @Test
    fun workflowRuleReorderHangingSeparator() {
        doTest()
    }

    @Test
    fun workflowRuleReorderInvalid() {
        doTest()
    }

    @Test
    @Ignore(value = "See https://github.com/JetBrains-Research/snakecharm/issues/30")
    fun workflowLocalrules() {
        doTest()
    }

    @Test
    fun workflowLocalrulesInvalid() {
        doTest()
    }

    @Test
    fun workflowLocalrulesHangingComma() {
        doTest()
    }

    /**
     * Test with latest versions of Python 2 and Python 3.
     */
    private fun doTest() {
        doTest(LanguageLevel.fromPythonVersion("2"))
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