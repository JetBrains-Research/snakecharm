package com.jetbrains.snakecharm.fixtures

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.jetbrains.snakecharm.SnakemakeTestCase

abstract class SnakemakeInspectionTestCase : SnakemakeTestCase() {
    protected abstract val inspectionClass: Class<out LocalInspectionTool>

    protected val testFilePath: String
        get() = testCaseDirectory + getTestName(isLowerCaseTestFile) + ".smk"

    protected val testDirectoryPath: String
        get() = testCaseDirectory + getTestName(false)

    protected val testCaseDirectory: String
        get() = "inspections/" + inspectionClass.simpleName + "/"

    protected val isLowerCaseTestFile: Boolean
        get() = true

    protected val isWeakWarning: Boolean
        get() = true

    protected val isInfo: Boolean
        get() = false

    protected val isWarning: Boolean
        get() = true

    override fun setUp() {
        super.setUp()
        InspectionProfileImpl.INIT_INSPECTIONS = true
    }

    override fun tearDown() {
        InspectionProfileImpl.INIT_INSPECTIONS = false
        super.tearDown()
    }

    /**
     * Launches test. To be called by test author
     */
    protected fun doTest() {
        fixture?.configureByFile(testFilePath)
        configureInspection()
    }

    protected fun configureInspection() {
        fixture?.enableInspections(inspectionClass)
        fixture?.checkHighlighting(isWarning, isInfo, isWeakWarning)
    }
}
