package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.jetbrains.snakecharm.fixtures.SnakemakeInspectionTestCase

class SnakemakeResourcesKeywordArgsInspectionTest : SnakemakeInspectionTestCase() {
    override val inspectionClass: Class<out LocalInspectionTool>
        get() = SnakemakeResourcesKeywordArgsInspection::class.java

    fun testResourcesAllArgsKeyword() {
        doTest()
    }

    fun testResourcesPositionalArgument() {
        doTest()
    }

    fun testResourcesPositionalAndKeywordArgs() {
        doTest()
    }
}