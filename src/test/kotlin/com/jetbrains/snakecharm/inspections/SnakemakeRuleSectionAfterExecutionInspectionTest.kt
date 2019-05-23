package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.jetbrains.snakecharm.fixtures.SnakemakeInspectionTestCase

class SnakemakeRuleSectionAfterExecutionInspectionTest : SnakemakeInspectionTestCase() {
    override val inspectionClass: Class<out LocalInspectionTool>
        get() = SnakemakeRuleSectionAfterExecutionInspection::class.java

    fun testNoRuleKeywordsAfterShell() {
        doTest()
    }

    fun testParamsKeywordAfterShell() {
        doTest()
    }

    fun testParamsKeywordAfterCWL() {
        doTest()
    }

    fun testThreadsKeywordAfterWrapper() {
        doTest()
    }

    fun testLogKeywordAfterScript() {
        doTest()
    }
}
