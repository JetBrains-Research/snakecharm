package com.jetbrains.snakecharm.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.jetbrains.snakecharm.fixtures.SnakemakeInspectionTestCase

class SmkShadowSettingsInspectionTest : SnakemakeInspectionTestCase() {
    override val inspectionClass: Class<out LocalInspectionTool>
        get() = SmkShadowSettingsInspection::class.java

    fun testWrongSetting() {
        doTest()
    }

    fun testCorrectSetting() {
        doTest()
    }
}