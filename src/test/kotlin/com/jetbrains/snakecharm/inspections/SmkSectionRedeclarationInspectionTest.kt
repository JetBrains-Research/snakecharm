package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.jetbrains.snakecharm.fixtures.SnakemakeInspectionTestCase

class SmkSectionRedeclarationInspectionTest : SnakemakeInspectionTestCase() {
    override val inspectionClass: Class<out LocalInspectionTool>
        get() = SmkSectionRedeclarationInspection::class.java

    fun testNoSectionRedeclarations() {
        doTest()
    }

    fun testSingleSectionRedeclaration() {
        doTest()
    }

    fun testMultipleSectionRedeclarations() {
        doTest()
    }
}