package com.jetbrains.snakecharm

/**
 * @author Roman.Chernyatchik
 * @date 2019-02-03
 */
class SnakemakeFoldingTest : SnakemakeTestCase() {
    private fun doTest() {
        val testDataRoot = SnakemakeTestUtil.getTestDataPath()
        val testName = getTestName(true)
        fixture!!.testFolding("$testDataRoot/folding/$testName.smk")
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
    
    fun testCheckpoint() {
        doTest()
    }

    fun testWorkflowTopLevel() {
        doTest()
    }
}