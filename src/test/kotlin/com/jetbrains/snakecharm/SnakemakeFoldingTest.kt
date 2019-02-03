package com.jetbrains.snakecharm

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */
class SnakemakeFoldingTest : SnakemakeTestCase() {
    //TODO see PyFoldingTest

    private fun doTest() {
        val testDataDir = SnakemakeTestUtil.getTestDataPath().resolve("folding")
        fixture!!.testFolding("$testDataDir/${getTestName(true)}.smk")
    }

    fun testPythonBlocks() {
        doTest()
    }

    fun testPythonComments() {
        doTest()
    }

    fun testRule() {
        doTest()
    }

    fun testWorkflowTopLevel() {
        doTest()
    }
}