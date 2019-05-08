package com.jetbrains.snakecharm

class SnakemakeCompletionTest : SnakemakeTestCase() {
    
    private fun doTest(fileExtension: String = ".smk") {
        val testName = getTestName(true)
        fixture?.configureByFile("completion/$testName$fileExtension")
        fixture?.completeBasic()
        fixture?.checkResultByFile("completion/$testName.after$fileExtension")
    }
    
    fun testExpandRule() {
        doTest()
    }

    fun testExpandTopLevel() {
        doTest()
    }

    fun testExpandRunSection() {
        doTest()
    }

    fun testExpandPyFile() {
        doTest(".py")
    }

    fun testExpandCalledAsObjectMethod() {
        doTest()
    }

    fun testExpandDocstring() {
        doTest()
    }

    fun testExpandPyiFile() {
        doTest(".pyi")
    }

    // currently fails
    /*fun testExpandEmptyContext() {
        doTest()
    }*/
}