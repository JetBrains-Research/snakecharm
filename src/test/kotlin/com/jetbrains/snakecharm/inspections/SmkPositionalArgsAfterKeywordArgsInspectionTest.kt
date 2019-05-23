package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.jetbrains.snakecharm.fixtures.SnakemakeInspectionTestCase

class SmkPositionalArgsAfterKeywordArgsInspectionTest : SnakemakeInspectionTestCase() {
    override val inspectionClass: Class<out LocalInspectionTool>
        get() = SmkPositionalArgsAfterKeywordArgsInspection::class.java

    fun testPositionalArgumentsOnly() {
        doTest()
    }

    fun testKeywordArgumentsOnly() {
        doTest()
    }

    fun testPositionalArgsAfterKeywordArgs() {
        doTest()
    }

    fun testPositionalArgsBeforeKeywordArgs() {
        doTest()
    }

}
