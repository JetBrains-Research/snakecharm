package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.jetbrains.snakecharm.fixtures.SnakemakeInspectionTestCase

class SmkShadowMultipleSettingsInspectionTest : SnakemakeInspectionTestCase() {
    override val inspectionClass: Class<out LocalInspectionTool>
        get() = SmkShadowMultipleSettingsInspection::class.java

    fun testMultipleSettings() {
        doTest()
    }

    fun testSingleSetting() {
        doTest()
    }
}