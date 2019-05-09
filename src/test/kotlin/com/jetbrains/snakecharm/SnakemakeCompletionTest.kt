package com.jetbrains.snakecharm

class SnakemakeCompletionTest : SnakemakeTestCase() {
    
    private fun doTest(fileExtension: String = ".smk") {
        val testName = getTestName(true)
        fixture?.testCompletion("completion/$testName$fileExtension",
                "completion/$testName.after$fileExtension")
    }

    private fun checkCompletionListForString(string: String, expectedFilename: String) {
        val testName = getTestName(true)
        // it's necessary to do this to get the lookup first, then we can address lookup elements for the fixture
        val variants = fixture?.getCompletionVariants("completion/$testName.smk") ?: emptyList()
        assertTrue(variants.contains(string))
        val lookupElements = fixture?.lookupElements?.filter { it.lookupString == string } ?: emptyList()
        lookupElements.forEach { assertTrue(it.psiElement?.containingFile?.name == expectedFilename) }
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
        checkCompletionListForString("expand", "io.py")
    }

    fun testExpandNoPrefixAfterInput() {
        checkCompletionListForString("expand", "io.py")
    }
}