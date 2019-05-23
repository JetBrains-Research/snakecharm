package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.jetbrains.snakecharm.fixtures.SnakemakeInspectionTestCase

class SnakemakeMultipleExecutionSectionsInspectionTest : SnakemakeInspectionTestCase() {
    override val inspectionClass: Class<out LocalInspectionTool>
        get() = SnakemakeMultipleExecutionSectionsInspection::class.java

    fun testSingleShellKeyword() {
        doTest()
    }

    fun testSingleRunKeyword() {
        doTest()
    }

    fun testShellAndRunKeywords() {
        doTest()
    }

    fun testScriptAndWrapperAndCWLKeywords() {
        doTest()
    }
}