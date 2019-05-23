package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.jetbrains.snakecharm.fixtures.SnakemakeInspectionTestCase

class SmkResourcesKeywordArgsInspectionTest : SnakemakeInspectionTestCase() {
    override val inspectionClass: Class<out LocalInspectionTool>
        get() = SmkResourcesKeywordArgsInspection::class.java

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