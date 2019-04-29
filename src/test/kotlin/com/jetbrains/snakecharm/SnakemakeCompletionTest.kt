package com.jetbrains.snakecharm

import com.intellij.codeInsight.completion.impl.CamelHumpMatcher

class SnakemakeCompletionTest : SnakemakeTestCase() {
    
    private fun doTest(fileExtension: String = ".smk") {
        // TODO remove call to deprecated method
        CamelHumpMatcher.forceStartMatching(fixture?.testRootDisposable)
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

    /*fun testExpandPyDoc() {
        doTest()
    }*/

    fun testExpandPyiFile() {
        doTest(".pyi")
    }
}