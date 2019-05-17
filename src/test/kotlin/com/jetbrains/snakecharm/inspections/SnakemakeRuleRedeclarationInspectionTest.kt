package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.jetbrains.snakecharm.fixtures.SnakemakeInspectionTestCase

class SnakemakeRuleRedeclarationInspectionTest : SnakemakeInspectionTestCase() {
    override val inspectionClass: Class<out LocalInspectionTool>
        get() = SnakemakeRuleRedeclarationInspection::class.java

    fun testNoRuleRedeclarations() {
        doTest()
    }

    fun testSingleRuleRedeclaration() {
        doTest()
    }

    fun testMultipleRuleRedeclarations() {
        doTest()
    }
}