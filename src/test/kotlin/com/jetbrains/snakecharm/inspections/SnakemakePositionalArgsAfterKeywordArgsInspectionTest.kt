package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.jetbrains.snakecharm.fixtures.SnakemakeInspectionTestCase

class SnakemakePositionalArgsAfterKeywordArgsInspectionTest : SnakemakeInspectionTestCase() {
    override val inspectionClass: Class<out LocalInspectionTool>
        get() = SnakemakePositionalArgsAfterKeywordArgsInspection::class.java

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
