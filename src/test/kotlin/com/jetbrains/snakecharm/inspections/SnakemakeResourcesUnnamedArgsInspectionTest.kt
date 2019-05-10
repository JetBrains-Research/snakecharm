package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.jetbrains.snakecharm.fixtures.SnakemakeInspectionTestCase

class SnakemakeResourcesUnnamedArgsInspectionTest : SnakemakeInspectionTestCase() {
    override val inspectionClass: Class<out LocalInspectionTool>
        get() = SnakemakeResourcesUnnamedArgsInspection::class.java

    fun testResourcesAllArgsNamed() {
        doTest()
    }

    fun testResourcesUnnamedArgument() {
        doTest()
    }

    fun testResourcesNamedAndUnnamedArgs() {
        doTest()
    }
}