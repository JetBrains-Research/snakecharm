package com.jetbrains.snakecharm

class SnakemakeCompletionTest : SnakemakeTestCase() {
    
    private fun doTest(fileExtension: String = ".smk") {
        val testName = getTestName(true)
        fixture?.testCompletion("completion/$testName$fileExtension",
                "completion/$testName.after$fileExtension")
    }

    private fun checkCompletionListForString(string: String) {
        val testName = getTestName(true)
        val variants = fixture?.getCompletionVariants("completion/$testName.smk") ?: emptyList()
        assertTrue(variants.contains(string))
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

    fun testExpandEmptyContext() {
        checkCompletionListForString("expand")
    }

    fun testExpandNoPrefixAfterInput() {
        checkCompletionListForString("expand")
    }
}