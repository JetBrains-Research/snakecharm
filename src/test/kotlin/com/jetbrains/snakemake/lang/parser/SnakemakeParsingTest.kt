package com.jetbrains.snakemake.lang.parser

import com.intellij.openapi.application.PathManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ParsingTestCase
import com.jetbrains.python.PythonDialectsTokenSetContributor
import com.jetbrains.python.PythonDialectsTokenSetProvider
import com.jetbrains.python.PythonParserDefinition
import com.jetbrains.python.PythonTokenSetContributor
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.snakemake.lang.SnakemakeTokenSetContributor
import java.io.File
import java.nio.file.Path


/**
 * @author Roman.Chernyatchik
 * @date 2018-12-31
 */
class SnakemakeParsingTest : ParsingTestCase(
        "psi", "smk", SnakemakeParserDefinition(), PythonParserDefinition()
) {
    private var myLanguageLevel = LanguageLevel.getDefault()

    override fun setUp() {
        super.setUp()
        registerExtensionPoint(PythonDialectsTokenSetContributor.EP_NAME, PythonDialectsTokenSetContributor::class.java)
        registerExtension(PythonDialectsTokenSetContributor.EP_NAME, PythonTokenSetContributor())
        registerExtension(PythonDialectsTokenSetContributor.EP_NAME, SnakemakeTokenSetContributor())
        PythonDialectsTokenSetProvider.reset()
    }

    override fun getTestDataPath(): String {
        val homePath = projectHomePath(SnakemakeParsingTest::class.java)
        checkNotNull(homePath)
        return homePath.resolve("testData").toString()
    }

    private fun projectHomePath(aClass: Class<*>): Path? {
        val rootPath = PathManager.getResourceRoot(
                aClass,
                "/" + aClass.name.replace('.', '/') + ".class"
        )
        return when (rootPath) {
            null -> null
            else -> File(rootPath).toPath().parent.parent.parent
        }
    }

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

    fun testRuleNoName() {
        doTest()
    }

    fun testRuleMultiple() {
        doTest()
    }

    fun testRuleParams() {
        doTest()
    }

    fun testRuleInvalid1() {
        doTest()
    }

    fun testRuleUnexpKeyword() {
        doTest()
    }

    fun testRuleParamsListArgs() {
        doTest()
    }

    fun testRuleParamsListArgsMultiple() {
        doTest()
    }

    fun testRuleParamsListKeywordArgs() {
        doTest()
    }

    fun testRuleParamsListKeywordArgsMultiple() {
        doTest()
    }
    //
    //
    //
    //

//    fun testFoo() {
//        TODO dooo
//        doTest()
//    }

    /**
     * Test with latest versions of Python 2 and Python 3.
     */
    private fun doTest() {
//        doTest(LanguageLevel.fromPythonVersion("2"))
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